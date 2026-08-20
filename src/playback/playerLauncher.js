import { Alert } from 'react-native';

/**
 * The entire playback surface of the frontend. The custom player replaces
 * this module; nothing else in the app knows how playback works.
 */
export const playerLauncher = {
    play(item, origin) {
        Alert.alert('Player not installed yet', `${item.Name} (from ${origin})`);
    }
};
