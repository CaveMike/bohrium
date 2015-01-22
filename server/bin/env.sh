#!/bin/bash

# Set GAE_HOME to the root of the Google AppEngine install.
# On MacOS:
export GAE_HOME="/Applications/GoogleAppEngineLauncher.app/Contents/Resources/GoogleAppEngine-default.bundle/Contents/Resources/google_appengine/"

export PYTHONPATH="${GAE_HOME}:./src/:./cfg"

