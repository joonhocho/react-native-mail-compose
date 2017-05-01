/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View,
  TouchableOpacity
} from 'react-native';
import MailCompose from 'react-native-mail-compose';

export default class ExampleApp extends Component {
  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!
        </Text>
        <Text style={styles.instructions}>
          To get started, edit index.android.js
        </Text>
        <Text style={styles.instructions}>
          Double tap R on your keyboard to reload,{'\n'}
          Shake or press menu button for dev menu
        </Text>
        <TouchableOpacity onPress={async () => {
          try {
            const res = await MailCompose.send({
              toRecipients: ['rnmailcompose1@gmail.com', 'rnmailcompose2@gmail.com'],
              ccRecipients: ['rnmailcompose3@gmail.com', 'rnmailcompose4@gmail.com'],
              bccRecipients: ['rnmailcompose5@gmail.com', 'rnmailcompose6@gmail.com'],
              subject: 'This is text subject',
              html: '<p>This <b>is</b> text body</p>',
              attachments: [{
                filename: 'mytext',
                ext: '.txt',
                mimeType: 'text/plain',
                text: 'Hello my friend',
              }],
            });
            console.log(res);
          } catch (e) {
            console.error('error', e);
          }
        }}>
          <Text>
            Test
          </Text>
        </TouchableOpacity>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});

AppRegistry.registerComponent('ExampleApp', () => ExampleApp);
