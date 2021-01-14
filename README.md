# BixbyAsPowerButton

To enable make sure that you have disabled Bixby:
```shell script
adb shell pm disable-user com.samsung.android.bixby.agent
```

And enable logcat reading for this app:
```shell script
adb shell pm grant dr.ramm.bixbyfix android.permission.READ_LOGS
```


Then enable the accessibility service in settings:
- Open settings
- Navigate to `Accessibility`
- Then `Installed services`
- Tap `Bixby as Power Button`
- Enable at the top of the screen.
- Enjoy your new power button.
