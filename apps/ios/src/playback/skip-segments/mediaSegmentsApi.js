export async function fetchMediaSegments(client, itemId, includeSegmentTypes) {
    const result = await client.get(`/MediaSegments/${itemId}`, {
        includeSegmentTypes: includeSegmentTypes.join(',')
    });
    return result.Items ?? [];
}
