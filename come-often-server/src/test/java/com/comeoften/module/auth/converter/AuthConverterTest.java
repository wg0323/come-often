package com.comeoften.module.auth.converter;

import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.entity.SysMenu;
import com.comeoften.module.auth.entity.SysUser;
import com.comeoften.module.auth.vo.MemberInfoVO;
import com.comeoften.module.auth.vo.MenuVO;
import com.comeoften.module.auth.vo.UserInfoVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/** 认证域对象转换：非等价字段映射、多源合并与手机号脱敏。 */
class AuthConverterTest {

    @Test
    void should_map_menu_fields_when_converting() {
        SysMenu menu = new SysMenu();
        menu.setId(100L);
        menu.setParentId(0L);
        menu.setMenuName("经营看板");
        menu.setMenuType(1);
        menu.setPerm("dashboard:view");
        menu.setPath("/");
        menu.setSort(1);

        MenuVO vo = AuthConverter.toMenuVO(menu);

        assertThat(vo.getName()).isEqualTo("经营看板");
        assertThat(vo.getPath()).isEqualTo("/");
        assertThat(vo.getPerm()).isEqualTo("dashboard:view");
        assertThat(vo.getType()).isEqualTo(1);
        assertThat(vo.getId()).isEqualTo(100L);
    }

    @Test
    void should_merge_user_and_permissions_when_converting_info() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("超级管理员");

        UserInfoVO vo = AuthConverter.toUserInfoVO(user, 1L,
                Collections.singletonList("SUPER_ADMIN"), Arrays.asList("dashboard:view", "system:user"));

        assertThat(vo.getUsername()).isEqualTo("admin");
        assertThat(vo.getStoreId()).isEqualTo(1L);
        assertThat(vo.getRoles()).containsExactly("SUPER_ADMIN");
        assertThat(vo.getPermissions()).containsExactly("dashboard:view", "system:user");
    }

    @Test
    void should_mask_middle_digits_when_phone_is_full() {
        Member member = new Member();
        member.setId(9L);
        member.setPhone("13800000001");
        member.setNickname("微信用户");

        MemberInfoVO vo = AuthConverter.toMemberInfoVO(member);

        assertThat(vo.getPhone()).isEqualTo("138****0001");
        assertThat(vo.getMemberId()).isEqualTo(9L);
    }

    @Test
    void should_return_original_phone_when_phone_is_too_short() {
        Member member = new Member();
        member.setPhone("123");

        assertThat(AuthConverter.toMemberInfoVO(member).getPhone()).isEqualTo("123");
    }
}
