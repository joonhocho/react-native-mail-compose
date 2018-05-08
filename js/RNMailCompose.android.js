import {NativeModules} from 'react-native';
import formatData from './formatData';


const {RNMailCompose} = NativeModules;

export default {
  name: RNMailCompose.name,
  hasMailApp(appName) {
    return RNMailCompose.hasMailApp(appName)
  },
  getMailAppData() {
    return RNMailCompose.getMailAppData()
  },
  getLastSelection() {
    return RNMailCompose.getLastSelection()
  },
  send(data) {
    return RNMailCompose.send(data)
  }
};
