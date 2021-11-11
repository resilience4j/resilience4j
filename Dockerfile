FROM gitpod/workspace-full

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && sdk install java 11.0.13-tem"