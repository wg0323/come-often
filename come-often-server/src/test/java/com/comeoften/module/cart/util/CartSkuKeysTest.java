package com.comeoften.module.cart.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CartSkuKeysTest {

    @Test
    void should_sort_and_deduplicate_when_building_sku_key() {
        assertThat(CartSkuKeys.build(Arrays.asList(3004L, 3001L, 3004L)))
                .isEqualTo("3001,3004");
    }

    @Test
    void should_return_empty_string_when_ids_are_absent() {
        assertThat(CartSkuKeys.build(null)).isEmpty();
        assertThat(CartSkuKeys.build(Collections.emptyList())).isEmpty();
    }

    @Test
    void should_round_trip_when_parsing_key_back_to_ids() {
        String key = CartSkuKeys.build(Arrays.asList(4001L, 3002L));
        assertThat(CartSkuKeys.parse(key)).containsExactly(3002L, 4001L);
        assertThat(CartSkuKeys.parse("")).isEmpty();
        assertThat(CartSkuKeys.parse(null)).isEmpty();
    }

    @Test
    void should_produce_same_key_when_id_order_differs() {
        assertThat(CartSkuKeys.build(Arrays.asList(1L, 2L)))
                .isEqualTo(CartSkuKeys.build(Arrays.asList(2L, 1L)));
    }
}
