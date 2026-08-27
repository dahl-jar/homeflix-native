import { OrientationLock, lockAsync, unlockAsync } from 'expo-screen-orientation';
import { useEffect } from 'react';

export function allowPlayerRotation() {
    return unlockAsync();
}

export function restoreAppPortrait() {
    return lockAsync(OrientationLock.PORTRAIT_UP);
}

export function useRouteOrientation(allowsRotation) {
    useEffect(() => {
        const changeOrientation = allowsRotation ? allowPlayerRotation : restoreAppPortrait;
        void changeOrientation();
    }, [allowsRotation]);
}
