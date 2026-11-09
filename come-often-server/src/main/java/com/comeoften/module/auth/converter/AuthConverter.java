package com.comeoften.module.auth.converter;

import com.comeoften.common.MaskUtils;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.entity.SysMenu;
import com.comeoften.module.auth.entity.SysUser;
import com.comeoften.module.auth.vo.MemberInfoVO;
import com.comeoften.module.auth.vo.MenuVO;
import com.comeoften.module.auth.vo.UserInfoVO;

import java.util.List;

/**
 * 认证域对象转换：字段名不同与多源合并集中在此，手机号脱敏复用 {@link MaskUtils}。
 */
public final class AuthConverter {

    private AuthConverter() {
    }

    public static MenuVO toMenuVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setPerm(menu.getPerm());
        vo.setType(menu.getMenuType());
        vo.setSort(menu.getSort());
        return vo;
    }

    public static UserInfoVO toUserInfoVO(SysUser user, Long storeId,
                                          List<String> roles, List<String> permissions) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setStoreId(storeId);
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }

    public static MemberInfoVO toMemberInfoVO(Member member) {
        MemberInfoVO vo = new MemberInfoVO();
        vo.setMemberId(member.getId());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setPhone(MaskUtils.maskPhone(member.getPhone()));
        vo.setLevelId(member.getLevelId());
        return vo;
    }
}
