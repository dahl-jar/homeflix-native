const NEGOTIATION_KEYS = [
    'platform',
    'preferredMediaSourceId',
    'serverUrl',
    'userId'
];

function matchesPlayback(entry, options) {
    if (entry.options.player !== options.player) return false;
    if (entry.options.negotiationOptions.item.Id !== options.negotiationOptions.item.Id) {
        return false;
    }
    return NEGOTIATION_KEYS.every((key) =>
        Object.is(
            entry.options.negotiationOptions[key],
            options.negotiationOptions[key]
        )
    );
}

export function createPlaybackRuntimeLease({ createRuntime, registry }) {
    let entry = null;

    return {
        acquire(options, onSnapshot, runtimeFactory = createRuntime) {
            if (!entry || !matchesPlayback(entry, options)) {
                const nextEntry = {
                    listener: onSnapshot,
                    options,
                    runtime: null
                };
                nextEntry.runtime = runtimeFactory({
                    ...options,
                    onSnapshot: (snapshot) => nextEntry.listener?.(snapshot)
                });
                entry = nextEntry;
            } else {
                entry.listener = onSnapshot;
            }
            const runtime = entry.runtime;
            onSnapshot(runtime.getSnapshot());
            void registry.activate(runtime).catch(() => {});
            return runtime;
        },
        release(runtime) {
            if (entry?.runtime === runtime) entry.listener = null;
            return registry.scheduleDeactivate(runtime);
        },
        deactivate(runtime) {
            if (entry?.runtime === runtime) entry.listener = null;
            return registry.deactivate(runtime);
        }
    };
}
