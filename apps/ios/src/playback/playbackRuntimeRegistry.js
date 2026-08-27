export function createPlaybackRuntimeRegistry() {
    let activeRuntime = null;
    let pendingDeactivation = null;

    return {
        async activate(runtime) {
            if (pendingDeactivation === runtime) pendingDeactivation = null;
            if (activeRuntime === runtime) return;
            const previousRuntime = activeRuntime;
            activeRuntime = runtime;
            await previousRuntime?.stop();
            if (activeRuntime !== runtime) return;
            try {
                await runtime.start();
            } catch (error) {
                if (activeRuntime === runtime) activeRuntime = null;
                throw error;
            }
        },
        async scheduleDeactivate(runtime) {
            pendingDeactivation = runtime;
            await Promise.resolve();
            if (pendingDeactivation !== runtime) return;
            pendingDeactivation = null;
            if (activeRuntime !== runtime) return;
            activeRuntime = null;
            await runtime.stop();
        },
        async deactivate(runtime) {
            if (pendingDeactivation === runtime) pendingDeactivation = null;
            if (activeRuntime !== runtime) return;
            activeRuntime = null;
            await runtime.stop();
        }
    };
}

export const playbackRuntimeRegistry = createPlaybackRuntimeRegistry();
