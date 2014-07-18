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

ti sdk install https://github.com/teamcarma/titanium_mobile/releases/download/3_2_1_Alpha/mobilesdk-3.2.1.Alpha-osx.zip

```

**Fix ios-sim**

If having any problem with running ios-sim after the installation:

```

brew uninstall ios-sim; brew install ios-sim;

rm "/Users/$USER/Library/Application Support/Titanium/mobilesdk/osx/3.2.1.Alpha/iphone/ios-sim";
ln -s "/usr/local/Cellar/ios-sim/1.9.0/bin/ios-sim" "/Users/$USER/Library/Application Support/Titanium/mobilesdk/osx/3.2.1.Alpha/iphone/ios-sim";

```

Dependencies
-----------

```

# install ios-sim
brew install ios-sim

```

