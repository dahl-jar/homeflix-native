import { primaryUrl } from '../../api/images/imageUrl.js';
import { PosterCard } from '../PosterCard/PosterCard.js';

export function MediaPosterCard({ item, serverUrl, imageWidth, ...cardProps }) {
    return (
        <PosterCard
            {...cardProps}
            item={item}
            imageUri={primaryUrl(serverUrl, item, imageWidth)}
        />
    );
}
