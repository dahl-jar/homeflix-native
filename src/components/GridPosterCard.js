import { useRouter } from 'expo-router';

import { MediaPosterCard } from './MediaPosterCard.js';

const GRID_IMAGE_WIDTH = 300;

export function GridPosterCard({ item, serverUrl, width, showTitle = false, onPress }) {
    const router = useRouter();
    const handlePress = onPress ?? (() => router.push(`/details/${item.Id}`));

    return (
        <MediaPosterCard
            item={item}
            serverUrl={serverUrl}
            imageWidth={GRID_IMAGE_WIDTH}
            width={width}
            showTitle={showTitle}
            onPress={handlePress}
        />
    );
}
