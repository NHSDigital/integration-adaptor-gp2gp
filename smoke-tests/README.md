# Smoke Tests

## Running the smoke test

1. Make sure the environment is set up
2. Ensure you are in the `smoketests` directory
3. Run `.\run-smoke-tests.sh <path to your vars.sh script>`, where the parameter is the location of your configuration shell script.

#### Troubleshooting on M1 mac

Issue: `zsh: permission denied: ./run-smoke-tests.sh`

Resolution: to give smoke tests script the permission to run - in your terminal run `chmod +x run-smoke-tests.sh`

----

Issue: `zsh: permission denied: ./gradlew`

Resolution: to give gradlew the permission to run - in your terminal run `chmod +x gradlew`

----

Issue: `Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

Resolution: ensure gradle is installed and the right version within the smoketest directory - run `brew install gradle`
