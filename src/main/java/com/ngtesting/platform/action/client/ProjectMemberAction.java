package com.ngtesting.platform.action.client;

import com.alibaba.fastjson.JSONObject;
import com.ngtesting.platform.action.BaseAction;
import com.ngtesting.platform.config.Constant;
import com.ngtesting.platform.model.*;
import com.ngtesting.platform.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = Constant.API_PATH_CLIENT + "/project_member")
public class ProjectMemberAction extends BaseAction {
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectConfigService projectConfigService;

    @Autowired
    private ProjectRoleService projectRoleService;
    @Autowired
    private ProjectRoleEntityRelationService projectRoleEntityRelationService;
    @Autowired
    private HistoryService historyService;

    @Autowired
    private PushSettingsService pushSettingsService;

    @Autowired
    AuthService authService;

    @ResponseBody
    @PostMapping("/getUsers")
    public Map<String, Object> getUsers(HttpServletRequest request, @RequestBody JSONObject json) {
        Map<String, Object> ret = new HashMap<String, Object>();
        TstUser user = (TstUser) request.getSession().getAttribute(Constant.HTTP_SESSION_USER_PROFILE);
        Integer orgId = user.getDefaultOrgId();

        Integer projectId = json.getInteger("projectId");
        if (userNotInProject(user.getId(), projectId)) {
            return authFail();
        }

        List<TstProjectRole> projectRoles = projectRoleService.list(orgId, null, null);

        List<TstProjectRoleEntityRelation> entityInRoles = projectRoleEntityRelationService.listByProject(projectId);

        ret.put("projectRoles", projectRoles);
        ret.put("entityInRoles", entityInRoles);

        ret.put("code", Constant.RespCode.SUCCESS.getCode());
        return ret;
    }

    @PostMapping(value = "saveMembers")
    @ResponseBody
    public Map<String, Object> saveMembers(HttpServletRequest request, @RequestBody JSONObject json) {
        Map<String, Object> ret = new HashMap<String, Object>();
        TstUser user = (TstUser) request.getSession().getAttribute(Constant.HTTP_SESSION_USER_PROFILE);
        Integer orgId = user.getDefaultOrgId();

        Integer projectId = json.getInteger("projectId");
        if (userNotInProject(user.getId(), projectId)) {
            return authFail();
        }

        List<TstProjectRoleEntityRelation> entityInRoles = projectRoleEntityRelationService.batchSavePers(json, orgId);

        TstProject project = projectService.get(projectId);
        historyService.create(projectId, user, Constant.MsgType.update.msg,
                TstHistory.TargetType.project_member, projectId, project.getName());

        ret.put("entityInRoles", entityInRoles);
        ret.put("code", Constant.RespCode.SUCCESS.getCode());
        return ret;
    }

    @PostMapping(value = "changeRole")
    @ResponseBody
    public Map<String, Object> changeRole(HttpServletRequest request, @RequestBody JSONObject json) {
        Map<String, Object> ret = new HashMap<String, Object>();
        TstUser user = (TstUser) request.getSession().getAttribute(Constant.HTTP_SESSION_USER_PROFILE);

        Integer projectId = json.getInteger("projectId");
        if (userNotInProject(user.getId(), projectId)) {
            return authFail();
        }

        List<TstProjectRoleEntityRelation> entityInRoles = projectRoleEntityRelationService.changeRolePers(json);

        pushSettingsService.pushPrjSettings(user);

        ret.put("entityInRoles", entityInRoles);
        ret.put("code", Constant.RespCode.SUCCESS.getCode());
        return ret;
    }

    @PostMapping(value = "remove")
    @ResponseBody
    public Map<String, Object> remove(HttpServletRequest request, @RequestBody JSONObject json) {
        Map<String, Object> ret = new HashMap<String, Object>();
        TstUser user = (TstUser) request.getSession().getAttribute(Constant.HTTP_SESSION_USER_PROFILE);

        Integer projectId = json.getInteger("projectId");
        String type = json.getString("type");
        Integer entityId = json.getInteger("entityId");

        if (userNotInProject(user.getId(), projectId)) {
            return authFail();
        }

        List<TstProjectRoleEntityRelation> entityInRoles =
                projectRoleEntityRelationService.remove(projectId, type, entityId);

        pushSettingsService.pushPrjSettings(user);

        ret.put("entityInRoles", entityInRoles);
        ret.put("code", Constant.RespCode.SUCCESS.getCode());
        return ret;
    }

}
