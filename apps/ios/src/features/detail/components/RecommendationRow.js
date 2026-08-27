import { useRouter } from 'expo-router';

import { MediaPosterCard } from '../../../components/MediaPosterCard/MediaPosterCard.js';

import { HorizontalSection } from './HorizontalSection.js';

const CARD_WIDTH = 110;
const IMAGE_WIDTH = 220;

export function RecommendationRow({ items, serverUrl }) {
    const router = useRouter();
    if (items.length === 0) return null;

    return (
        <HorizontalSection title="More Like This">
            {items.map((item) => (
                <MediaPosterCard
                    key={item.Id}
                    item={item}
                    serverUrl={serverUrl}
                    imageWidth={IMAGE_WIDTH}
                    width={CARD_WIDTH}
                    onPress={() => router.push(`/details/${item.Id}`)}
                />
            ))}
        </HorizontalSection>
    );
}
