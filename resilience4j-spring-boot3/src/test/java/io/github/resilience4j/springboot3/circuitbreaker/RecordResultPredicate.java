package io.github.resilience4j.springboot3.circuitbreaker;

import org.apache.http.HttpStatus;

import java.util.function.Predicate;

public class RecordResultPredicate implements Predicate<Object> {
  @Override
  public boolean test(Object o) {
    return false;
  }
}
