const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export type ClientIdentity = {
    deviceId: string;
    version: string;
};

type ClientIdentityDependencies = {
    readDeviceId: () => Promise<string | null>;
    writeDeviceId: (deviceId: string) => Promise<void>;
    createDeviceId: () => string;
    version: string | undefined;
};

function validDeviceId(deviceId: string | null): deviceId is string {
    return typeof deviceId === 'string' && UUID_PATTERN.test(deviceId);
}

export async function resolveClientIdentity({
    readDeviceId,
    writeDeviceId,
    createDeviceId,
    version
}: ClientIdentityDependencies): Promise<ClientIdentity> {
    if (!version?.trim()) throw new Error('app version is unavailable');
    const storedDeviceId = await readDeviceId();
    if (validDeviceId(storedDeviceId)) {
        return { deviceId: storedDeviceId, version };
    }
    const deviceId = createDeviceId();
    if (!validDeviceId(deviceId)) throw new Error('device identity generator returned an invalid UUID');
    await writeDeviceId(deviceId);
    return { deviceId, version };
}
