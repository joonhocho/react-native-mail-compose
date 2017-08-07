# react-native-mail-compose
React Native library for composing email. Wraps MFMailComposeViewController for iOS and Intent for Android.

For composing text message, check out [joonhocho/react-native-message-compose](https://github.com/joonhocho/react-native-message-compose).


## Getting started

Tested with React Native 0.43.x.

`$ react-native install react-native-mail-compose`


## Android (Manual Installation)
Theses steps are automatically done by `react-native install`.

 - Add to your `{YourApp}/android/settings.gradle`:
```
include ':react-native-mail-compose'
project(':react-native-mail-compose').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-mail-compose/android')
...other modules
```

 - Modify your `{YourApp}/android/app/build.gradle`:
```
dependencies {
    compile project(':react-native-mail-compose') // Add this
    ...other modules
}
```

 - Modify your `{YourApp}/android/app/src/main/java/com/{YourApp}/MainApplication.java`:
```
...
import com.reactlibrary.mailcompose.RNMailComposePackage; // Add this
...
public class MainApplication extends Application implements ReactApplication {
...
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
          new RNMailComposePackage() // Add this
          ...other modules
      );
    }
```

## iOS (Required)
These steps MUST be done manually. They are NOT done by `react-native install`.

- Make sure you have a Swift Bridging Header for your project. Here's [how to create one](http://www.learnswiftonline.com/getting-started/adding-swift-bridging-header/) if you don't.
- Open up your project in xcode and right click the package.
- Click `Add files to '{YourApp}'`.
- Select to `{YourApp}/node_modules/react-native-mail-compose/ios/RNMailCompose`.
- Click 'Add'.


Add to your Swift Bridging Header, `{YourApp}/ios/{YourApp}-Bridging-Header.h`:
```
#import <React/RCTBridgeModule.h>
#import <React/RCTViewManager.h>
#import <React/RCTEventEmitter.h>
```

## Usage
```javascript
import MailCompose from 'react-native-mail-compose';

// later in your code...
async sendMail() {
  try {
    await MailCompose.send({
      toRecipients: ['to1@example.com', 'to2@example.com'],
      ccRecipients: ['cc1@example.com', 'cc2@example.com'],
      bccRecipients: ['bcc1@example.com', 'bcc2@example.com'],
      subject: 'This is subject',
      text: 'This is body',
      html: '<p>This is <b>html</b> body</p>', // Or, use this if you want html body. Note that some Android mail clients / devices don't support this properly.
      attachments: [{
        filename: 'mytext', // [Optional] If not provided, UUID will be generated.
        ext: '.txt',
        mimeType: 'text/plain',
        text: 'Hello my friend', // Use this if the data is in UTF8 text.
        data: '...BASE64_ENCODED_STRING...', // Or, use this if the data is not in plain text.
      }],
    });
  } catch (e) {
    // e.code may be 'cannotSendMail' || 'cancelled' || 'saved' || 'failed'
  }
}
```


## LICENSE
```
The MIT License (MIT)

Copyright (c) 2017 Joon Ho Cho

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
