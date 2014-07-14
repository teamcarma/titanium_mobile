Appcelerator Titanium Mobile 3.2.1.Alpha
============================

Build
-----------

```

scons

```


Installation
-----------

```

ti sdk install https://github.com/barbarum/titanium_mobile/releases/download/3_2_1_Alpha/mobilesdk-3.2.1.Alpha-osx.zip

# install ios-sim
brew install ios-sim

# Fix the ios-sim reference
rm "/Users/$USER/Library/Application Support/Titanium/mobilesdk/osx/3.2.1.Alpha/iphone/ios-sim"
ln -s "/usr/local/Cellar/ios-sim/1.9.0/bin/ios-sim" "/Users/$USER/Library/Application Support/Titanium/mobilesdk/osx/3.2.1.Alpha/iphone/ios-sim"

```




