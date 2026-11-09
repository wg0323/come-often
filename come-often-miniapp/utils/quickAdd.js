/**
 * 列表快捷加购：规格沿用详情页默认口径（每组第一项），加料不带；
 * 想换规格或加料仍需进详情页
 * @author come-often
 * @date 2026-11-09
 */
import { addToCart } from '../api/cartApi';
import { getDishDetail } from '../api/dishApi';

export const quickAddDish = async (dishId) => {
  const detail = await getDishDetail(dishId);
  const specOptionIds = (detail?.specGroups ?? [])
    .map((group) => group.options?.[0]?.id)
    .filter((optionId) => optionId);
  return addToCart({
    dishId, quantity: 1, specOptionIds, addonIds: [],
  });
};
