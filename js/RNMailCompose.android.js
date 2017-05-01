import {NativeModules} from 'react-native';
import formatData from './formatData';


const {RNMailCompose} = NativeModules;

export default {
  name: RNMailCompose.name,

  send(data) {
    return RNMailCompose.send(data);
  },
};
