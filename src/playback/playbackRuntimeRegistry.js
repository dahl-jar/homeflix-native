export function createPlaybackRuntimeRegistry() {
    let activeRuntime = null;

    return {
        async activate(runtime) {
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
        async deactivate(runtime) {
            if (activeRuntime !== runtime) return;
            activeRuntime = null;
            await runtime.stop();
        }
    };
}

export const playbackRuntimeRegistry = createPlaybackRuntimeRegistry();
