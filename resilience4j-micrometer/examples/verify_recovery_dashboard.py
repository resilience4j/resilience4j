#!/usr/bin/env python3
"""Check the dashboard structure and, optionally, its queries with promtool.

No Grafana or Prometheus service is contacted. The optional promtool checks use
generated synthetic series and read expressions directly from the dashboard.
"""

import argparse
import json
from pathlib import Path
import re
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[2]
DASHBOARD = ROOT / "grafana_recovery_dashboard.json"
PREFIX = "resilience4j_circuitbreaker_"
METRICS = {
    PREFIX + suffix for suffix in (
        "state", "recovery_phase", "recovery_traffic_percentage",
        "admission_attempts_total", "admission_permitted_attempts_total",
    )
}


def substitute(expression, selections=None):
    # These single-selection variables occur inside quoted exact matchers.
    # Encode fixture selections as PromQL string-literal contents. This tests
    # query semantics, not Grafana's browser interpolation: quote handling there
    # depends on the Grafana version/feature flags. The live lab separately
    # verifies the actual datasource interpolation for the demo's dotted names.
    selected = selections or {"job": "demo", "instance": "fixture-a", "circuitbreaker": "psp"}
    values = {"${" + key + "}": json.dumps(value, ensure_ascii=False)[1:-1]
              for key, value in selected.items()}
    values["$__rate_interval"] = "1m"
    for variable, value in values.items():
        expression = expression.replace(variable, value)
    assert "$" not in expression, "An unexpanded dashboard variable remains"
    return expression


def verify_structure(dashboard):
    assert dashboard["schemaVersion"] == 41
    assert dashboard["id"] is None
    assert dashboard["uid"] == "r4j-gradual-recovery"
    assert dashboard["__inputs"][0]["name"] == "DS_PROMETHEUS"
    assert dashboard["__inputs"][0]["pluginId"] == "prometheus"
    variables = dashboard["templating"]["list"]
    assert [v["name"] for v in variables] == ["job", "instance", "circuitbreaker"]
    assert all(v["multi"] is False and v["includeAll"] is False for v in variables)
    assert '${job}' in variables[1]["definition"]
    assert '${instance}' in variables[2]["definition"]
    panels = dashboard["panels"]
    assert len(panels) == 10
    assert len({p["id"] for p in panels}) == len(panels)
    occupied = set()
    expressions = []
    seen_metrics = set()
    for panel in panels:
        grid = panel["gridPos"]
        assert 0 <= grid["x"] < 24 and 1 <= grid["w"] <= 24
        assert grid["x"] + grid["w"] <= 24 and grid["h"] > 0
        cells = {(x, y) for x in range(grid["x"], grid["x"] + grid["w"])
                 for y in range(grid["y"], grid["y"] + grid["h"])}
        assert not cells.intersection(occupied), "Panel grid positions overlap"
        occupied.update(cells)
        if panel["type"] == "text":
            continue
        assert panel["datasource"] == {"type": "prometheus", "uid": "${DS_PROMETHEUS}"}
        references = [target["refId"] for target in panel["targets"]]
        assert len(references) == len(set(references))
        for target in panel["targets"]:
            assert target["datasource"] == panel["datasource"]
            expression = target["expr"]
            selectors = re.findall(r"(resilience4j_circuitbreaker_\w+)\{([^}]*)\}",
                                   expression.replace("${job}", "job-value")
                                   .replace("${instance}", "instance-value")
                                   .replace("${circuitbreaker}", "breaker-value"))
            assert selectors
            for metric, labels in selectors:
                assert metric in METRICS, metric
                seen_metrics.add(metric)
                assert all(label + '="' in labels for label in ("job", "instance", "name"))
                assert '=~' not in labels and ':regex' not in expression
                assert "phase=" not in labels and "percentage=" not in labels
            expressions.append(substitute(expression))
    assert seen_metrics == METRICS
    by_id = {p["id"]: p for p in panels}
    assert by_id[1]["options"]["textMode"] == "name"
    assert " > bool 0) or on (job, instance, name) " in by_id[2]["targets"][0]["expr"]
    assert " == 1" in by_id[1]["targets"][0]["expr"]
    assert " > 0)" in by_id[5]["targets"][0]["expr"]
    assert by_id[8]["targets"][0]["expr"].endswith(" > 0")
    assert "max" not in by_id[8]["fieldConfig"]["defaults"]
    return expressions


def series(metric, values, instance="fixture-a", state=None, name="psp", job="demo"):
    labels = {"job": job, "instance": instance, "name": name, "region": "eu"}
    if state is not None:
        labels["state"] = state
    encoded = ",".join(f'{key}={json.dumps(value, ensure_ascii=False)}'
                       for key, value in sorted(labels.items()))
    return {"series": metric + "{" + encoded + "}", "values": values}


def sample(value, metric=None, state=None, instance="fixture-a", name="psp", job="demo"):
    if state is not None:
        labels = '{state="' + state + '"}'
    elif metric is False:
        labels = "{" + ",".join(f"{key}={json.dumps(item, ensure_ascii=False)}"
                                 for key, item in sorted({"instance": instance, "job": job, "name": name}.items())) + "}"
    else:
        labels = (metric or "") + "{" + ",".join(
            f"{key}={json.dumps(item, ensure_ascii=False)}" for key, item in sorted(
                {"instance": instance, "job": job, "name": name, "region": "eu"}.items())) + "}"
    return {"labels": labels, "value": value}


def fixture_tests(dashboard):
    panels = {p["id"]: p for p in dashboard["panels"]}
    state_expr = substitute(panels[1]["targets"][0]["expr"])
    mode_expr = substitute(panels[2]["targets"][0]["expr"])
    phase_expr = substitute(panels[3]["targets"][0]["expr"])
    target_expr = substitute(panels[4]["targets"][0]["expr"])
    observed_expr = substitute(panels[5]["targets"][0]["expr"])
    plotted_target = substitute(panels[8]["targets"][0]["expr"])
    tests = []

    def check(expression, expected):
        return {"expr": expression, "eval_time": "2m", "exp_samples": expected}

    def input_state(active, instance="fixture-a"):
        return [series(PREFIX + "state", ("1" if active == value else "0") + "+0x8",
                       instance, value)
                for value in ("closed", "open", "half_open", "disabled", "forced_open", "metrics_only")]

    normal = input_state("half_open")
    tests.append({"name": "standard HALF_OPEN has no gradual instrumentation", "interval": "15s",
                  "input_series": normal, "promql_expr_test": [
                      check(state_expr, [sample(1, state="half_open")]),
                      check(mode_expr, [sample(0, metric=False)]),
                      check(phase_expr, []), check(target_expr, []), check(observed_expr, [])]})

    def recovery(name, active_state, phase, target, attempts, permitted, observed,
                 extra=None):
        inputs = input_state(active_state) + [
            series(PREFIX + "recovery_phase", str(phase) + "+0x8"),
            series(PREFIX + "recovery_traffic_percentage", str(target) + "+0x8"),
            series(PREFIX + "admission_attempts_total", attempts),
            series(PREFIX + "admission_permitted_attempts_total", permitted),
        ] + (extra or [])
        tests.append({"name": name, "interval": "15s", "input_series": inputs,
                      "promql_expr_test": [
                          check(state_expr, [sample(1, state=active_state)]),
                          check(mode_expr, [sample(1, metric=False)]),
                          check(phase_expr, [sample(phase, PREFIX + "recovery_phase")]),
                          check(target_expr, [sample(target, PREFIX + "recovery_traffic_percentage")]),
                          check(plotted_target, [] if target == 0 else
                                [sample(target, PREFIX + "recovery_traffic_percentage")]),
                          check(observed_expr, [] if observed is None else [sample(observed)])]})

    recovery("gradual target and observed admission agree", "half_open", 2, 60,
             "0+100x8", "0+60x8", 60)
    recovery("configured gradual policy remains visible in CLOSED", "closed", 0, 0,
             "0+100x8", "0+100x8", 100)
    recovery("no attempts means no observed percentage", "half_open", 1, 30,
             "100+0x8", "30+0x8", None)
    recovery("grants from earlier permission checks do not invent an observed percentage", "half_open", 1, 30,
             "100+0x8", "10+1x8", None)
    recovery("counter resets retain the observed admission ratio", "half_open", 2, 60,
             "0 100 200 300 400 500 0 100 200", "0 60 120 180 240 300 0 60 120", 60)
    other = input_state("open", "fixture-b") + [
        series(PREFIX + "recovery_phase", "9+0x8", "fixture-b"),
        series(PREFIX + "recovery_traffic_percentage", "90+0x8", "fixture-b"),
        series(PREFIX + "admission_attempts_total", "0+1000x8", "fixture-b"),
        series(PREFIX + "admission_permitted_attempts_total", "0+900x8", "fixture-b"),
    ]
    recovery("another instance cannot change the selected breaker's values", "half_open", 2, 60,
             "0+100x8", "0+60x8", 60, other)
    tests.append({"name": "missing scrape data does not masquerade as Standard", "interval": "15s",
                  "input_series": [], "promql_expr_test": [check(state_expr, []), check(mode_expr, [])]})
    # Regressions for real application names and exact selection. A dotted name
    # is a literal, not a regex; quotes/backslashes remain legal label contents.
    for name, other_name in (("DemoService.process", "DemoServiceXprocess"),
                             ('PSP "legacy" \\ staging', 'PSP legacy staging')):
        selected = {"job": "demo.with-dots", "instance": "app:8080", "circuitbreaker": name}
        expression = substitute(panels[3]["targets"][0]["expr"], selected)
        tests.append({"name": "modeled exact literal selection: " + name, "interval": "15s",
                      "input_series": [
                          series(PREFIX + "recovery_phase", "3+0x8", "app:8080", name=name, job=selected["job"]),
                          series(PREFIX + "recovery_phase", "19+0x8", "app:8080", name=other_name, job=selected["job"])],
                      "promql_expr_test": [check(expression, [sample(3, PREFIX + "recovery_phase",
                          instance="app:8080", name=name, job=selected["job"])])]})
    return {"rule_files": [], "evaluation_interval": "15s", "fuzzy_compare": True, "tests": tests}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--promtool", help="Optional path to official promtool for syntax and synthetic query tests")
    args = parser.parse_args()
    dashboard = json.loads(DASHBOARD.read_text(encoding="utf-8"))
    expressions = verify_structure(dashboard)
    print(f"Dashboard structure: PASS ({len(dashboard['panels'])} panels, {len(expressions)} queries)", flush=True)
    if not args.promtool:
        print("PromQL syntax and semantics: NOT RUN (supply --promtool /path/to/promtool)")
        return
    if not Path(args.promtool).is_file():
        parser.error("--promtool must point to an existing promtool executable")
    subprocess.run([args.promtool, "check", "config",
                    str(Path(__file__).with_name("prometheus-recovery.yml"))], check=True)
    with tempfile.TemporaryDirectory(prefix="r4j-dashboard-") as temporary:
        rules = Path(temporary) / "queries.json"
        rules.write_text(json.dumps({"groups": [{"name": "dashboard-syntax", "rules": [
            {"record": f"dashboard_example_query_{index}", "expr": expression}
            for index, expression in enumerate(expressions)]}]}), encoding="utf-8")
        fixtures = Path(temporary) / "fixtures.json"
        tests = fixture_tests(dashboard)
        fixtures.write_text(json.dumps(tests), encoding="utf-8")
        subprocess.run([args.promtool, "check", "rules", str(rules)], check=True)
        subprocess.run([args.promtool, "test", "rules", str(fixtures)], check=True)
        assertions = sum(len(test["promql_expr_test"]) for test in tests["tests"])
        print(f"PromQL syntax and synthetic evaluation: PASS ({len(tests['tests'])} scenarios, {assertions} assertions)")


if __name__ == "__main__":
    main()
