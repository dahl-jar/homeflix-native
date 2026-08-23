import { fetchItem } from '../../api/items.js';

export async function selectSearchItem({ client, userId, itemId, navigate }) {
    const item = await fetchItem(client, userId, itemId);
    if (!item?.Id) throw new Error('materialized item has no canonical id');
    navigate(`/details/${item.Id}`);
}
