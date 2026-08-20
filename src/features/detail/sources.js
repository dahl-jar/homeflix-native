export const AUTO_SOURCE_KEY = 'auto';

const BYTES_PER_GB = 1024 * 1024 * 1024;

function sizeText(size) {
    if (!size) return null;
    return `${(size / BYTES_PER_GB).toFixed(1)} GB`;
}

/**
 * Maps PlaybackInfo media sources onto picker options behind an Auto entry.
 * A single placeholder source (named like the item, no size) means discovery
 * has not run yet; only Auto is offered then.
 */
export function sourceOptions(mediaSources, itemName) {
    const auto = { key: AUTO_SOURCE_KEY, label: 'Auto (recommended)' };
    const real = (mediaSources ?? []).filter(
        (source) => !(source.Name === itemName && !source.Size)
    );
    return [
        auto,
        ...real.map((source) => ({
            key: source.Id,
            label: sizeText(source.Size)
                ? `${source.Name}  ·  ${sizeText(source.Size)}`
                : source.Name
        }))
    ];
}
