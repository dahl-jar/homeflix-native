const HOME_ROUTE = '/(tabs)/home';

export async function exitPlaybackRoute(router, restorePortrait) {
    await restorePortrait();
    if (router.canGoBack()) {
        router.back();
        return;
    }
    router.replace(HOME_ROUTE);
}
