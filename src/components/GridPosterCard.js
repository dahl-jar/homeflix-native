import { useRouter } from 'expo-router';

import { MediaPosterCard } from './MediaPosterCard.js';

const GRID_IMAGE_WIDTH = 300;

export function GridPosterCard({ item, serverUrl, width, showTitle = false }) {
    const router = useRouter();

    return (
        <MediaPosterCard
            item={item}
            serverUrl={serverUrl}
            imageWidth={GRID_IMAGE_WIDTH}
            width={width}
            showTitle={showTitle}
            onPress={() => router.push(`/details/${item.Id}`)}
        />
    );
}
