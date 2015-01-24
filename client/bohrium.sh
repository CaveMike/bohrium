#!/usr/bin/env bash

alias log='adb logcat -v threadtime | grep bohrium'
alias dlog='adb logcat -d -v threadtime | grep bohrium'
alias clog='adb logcat -c;adb logcat -v threadtime | grep bohrium'

alias lr='adb logcat -v threadtime | egrep -i "(bohrium)"'
alias ldr='adb logcat -d -v threadtime | egrep -i "(bohrium)"'
alias lcr='adb logcat -c;adb logcat -v threadtime | egrep -i "(bohrium)"'

alias logcept='adb logcat -d -v threadtime | egrep -i "(system.err|androidruntime|exception)"'

alias run="adb shell am start -a android.intent.action.MAIN -n com.mikecorrigan.bohrium/.ActivityMain"

# Device state
alias dockoff="adb shell am broadcast -a android.intent.action.DOCK_EVENT --ei android.intent.extra.DOCK_STATE 0"
alias dockdesk="adb shell am broadcast -a android.intent.action.DOCK_EVENT --ei android.intent.extra.DOCK_STATE 1"
alias dockcar="adb shell am broadcast -a android.intent.action.DOCK_EVENT --ei android.intent.extra.DOCK_STATE 2"
alias dockledesk="adb shell am broadcast -a android.intent.action.DOCK_EVENT --ei android.intent.extra.DOCK_STATE 3"
alias dockhedesk="adb shell am broadcast -a android.intent.action.DOCK_EVENT --ei android.intent.extra.DOCK_STATE 4"

alias densityreset="adb shell wm density reset"
alias density160="adb shell wm density 160"
alias density240="adb shell wm density 240"
alias density320="adb shell wm density 320"
alias density480="adb shell wm density 480"

alias sizereset="adb shell wm size reset"
alias sizesm="adb shell wm size 600x400"
