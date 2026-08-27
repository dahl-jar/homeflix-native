import Constants from 'expo-constants';
import { randomUUID } from 'expo-crypto';
import { getItemAsync, setItemAsync } from 'expo-secure-store';

import { resolveClientIdentity } from './clientIdentity.ts';

const DEVICE_ID_KEY = 'homeflix-device-id';

export function loadClientIdentity() {
    return resolveClientIdentity({
        readDeviceId: () => getItemAsync(DEVICE_ID_KEY),
        writeDeviceId: (deviceId) => setItemAsync(DEVICE_ID_KEY, deviceId),
        createDeviceId: randomUUID,
        version: Constants.expoConfig?.version
    });
}
