import { primaryUrl } from '../api/imageUrl.js';

import { PosterCard } from './PosterCard.js';

export function MediaPosterCard({ item, serverUrl, imageWidth, ...cardProps }) {
    return (
        <PosterCard
            {...cardProps}
            item={item}
            imageUri={primaryUrl(serverUrl, item, imageWidth)}
        />
    );
}
