package com.cskaoyan.duolai.clean.user.service.impl;

import com.cskaoyan.duolai.clean.common.utils.CollUtils;
import com.cskaoyan.duolai.clean.common.utils.NumberUtils;
import com.cskaoyan.duolai.clean.common.utils.ObjectUtils;
import com.cskaoyan.duolai.clean.common.utils.StringUtils;
import com.cskaoyan.duolai.clean.user.converter.ServeProviderSettingConverter;
import com.cskaoyan.duolai.clean.user.dao.mapper.ServeProviderSettingsMapper;
import com.cskaoyan.duolai.clean.user.dao.entity.ServeProviderDO;
import com.cskaoyan.duolai.clean.user.dao.entity.ServeProviderSettingsDO;
import com.cskaoyan.duolai.clean.user.dao.entity.ServeProviderSyncDO;
import com.cskaoyan.duolai.clean.user.request.ServePickUpReqDTO;
import com.cskaoyan.duolai.clean.user.request.ServeScopeCommand;
import com.cskaoyan.duolai.clean.user.dto.CertificationStatusDTO;
import com.cskaoyan.duolai.clean.user.dto.ServeProviderDTO;
import com.cskaoyan.duolai.clean.user.dto.ServeProviderSettingsDTO;
import com.cskaoyan.duolai.clean.user.dto.ServeSettingsStatusDTO;
import com.cskaoyan.duolai.clean.user.service.IServeProviderService;
import com.cskaoyan.duolai.clean.user.service.IServeProviderSettingsService;
import com.cskaoyan.duolai.clean.user.service.IServeProviderSyncService;
import com.cskaoyan.duolai.clean.common.enums.EnableStatusEnum;
import com.cskaoyan.duolai.clean.common.expcetions.DBException;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务人员/机构附属信息 服务实现类
 * </p>
 */
@Service
public class ServeProviderSettingsServiceImpl extends ServiceImpl<ServeProviderSettingsMapper, ServeProviderSettingsDO> implements IServeProviderSettingsService {

    @Resource
    private IServeProviderService serveProviderService;

    @Resource
    private IServeProviderSyncService serveProviderSyncService;


    @Resource
    ServeProviderSettingConverter serveProviderSettingConverter;


    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void add(Long id) {
        ServeProviderSettingsDO serveProviderSettingsDO = new ServeProviderSettingsDO();
        serveProviderSettingsDO.setId(id);
        if (baseMapper.insert(serveProviderSettingsDO) <= 0) {
            throw new DBException("请求失败");
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void setServeScope(ServeScopeCommand serveScopeCommand) {
        String[] latAndLon = serveScopeCommand.getLocation().split(",");
        // 经度
        double lon = NumberUtils.parseDouble(latAndLon[0]);
        // 纬度
        double lat = NumberUtils.parseDouble(latAndLon[1]);
        // 1.更新服务范围

        ServeProviderSettingsDO serveProviderSettingsDO = serveProviderSettingConverter
                .serveScopeCommandToServeProviderSettingsDO(serveScopeCommand);
        serveProviderSettingsDO.setId(UserContext.currentUserId());
        serveProviderSettingsDO.setLat(lat);
        serveProviderSettingsDO.setLon(lon);
        baseMapper.updateById(serveProviderSettingsDO);

        // 经纬度
        ServeProviderSyncDO serveProviderSyncDO = ServeProviderSyncDO.builder()
                .id(UserContext.currentUserId())
                .cityCode(serveScopeCommand.getCityCode())
                .lon(lon)
                .lat(lat)
                .build();
        serveProviderSyncService.updateById(serveProviderSyncDO);

    }

    @Override
    public ServeProviderSettingsDTO getServeScope() {
        Long currentUserId = UserContext.currentUserId();
        ServeProviderSettingsDO serveProviderSettingsDO = baseMapper.selectById(currentUserId);
        if (serveProviderSettingsDO == null) {
            return new ServeProviderSettingsDTO();
        }
        ServeProviderSettingsDTO serveScopeResDTO = serveProviderSettingConverter
                .serveProviderSettingsDOToDTO(serveProviderSettingsDO);

        if (ObjectUtils.isNotNull(serveProviderSettingsDO.getLon())) {
            serveScopeResDTO.setLocation(serveProviderSettingsDO.getLon() + "," + serveProviderSettingsDO.getLat());
        }

        return serveScopeResDTO;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void setPickUp(Long id, Integer canPickUp) {
        // 1.更新接单信息
        ServeProviderSettingsDO serveProviderSettingsDO = new ServeProviderSettingsDO();
        serveProviderSettingsDO.setId(id);
        serveProviderSettingsDO.setCanPickUp(canPickUp);
        baseMapper.updateById(serveProviderSettingsDO);

        // 同步es
        ServeProviderSyncDO serveProviderSyncDO = ServeProviderSyncDO.builder().id(id)
                .pickUp(canPickUp)
                .build();
        serveProviderSyncService.updateById(serveProviderSyncDO);

    }

    //返回的设置状态在前端需要使用
    @Override
    public ServeSettingsStatusDTO getSettingStatus() {

        //当前用户id(当前的服务从业者id)
        long currentUserId = UserContext.currentUserId();

        /*
            查询服务人员设置信息(serve_provider_settings), 并获取以下状态:
            1. 是否开启接单 canPickUp
            2. 是否设置服务范围（看经纬度，以及接单位置(intentionScope)是否设置） serveScopeSetted
            3. 是否设置技能(判断haveSkill） serveSkillSetted
            4. settingStatus(设置完了所有的状态) serve_provider
         */
        ServeProviderSettingsDO settings = baseMapper.selectById(currentUserId);
        boolean canPickUp = false;
        boolean serveScopeSetted = false;
        boolean serveSkillSetted = false;
        Integer settingsStatus = 0;
        Integer certificationStatus = 0;

        if (settings != null) {
            canPickUp = settings.getCanPickUp() != null && settings.getCanPickUp() == 1;

            serveScopeSetted = settings.getLon() != null && settings.getLat() != null && StringUtils.isNotBlank(settings.getIntentionScope());

            serveSkillSetted = settings.getHaveSkill() != null && settings.getHaveSkill() == 1;
        }

        ServeProviderDO serveProviderDO = serveProviderService.getById(currentUserId);
        if (serveProviderDO != null && serveProviderDO.getSettingsStatus() != null) {
            settingsStatus = serveProviderDO.getSettingsStatus();
        }


        /*
              获取认证状态(根据从业者id，work_certification, 最新的认证记录的状态), 如果没查询到记录，认证状态默认为0。
              可以调用该方法获取认证状态: serveProviderService.getCertificationStatus(currentUserId)
         */
        CertificationStatusDTO certificationStatusDTO = serveProviderService.getCertificationStatus(currentUserId);
        if (certificationStatusDTO != null && certificationStatusDTO.getCertificationStatus() != null) {
            certificationStatus = certificationStatusDTO.getCertificationStatus();
        }

//        Integer settingsStatus = 0;
//        Boolean serveSkillSetted = false;
//        Boolean serveScopeSetted = false;
//        Integer certificationStatus = -1;

        //认证通过，设置服务技能，设置服务范围 更新 首次设置状态为完成(settingsStatus == 0说明之前未设置完成状态)
        if (settingsStatus == 0 && serveSkillSetted && serveScopeSetted && certificationStatus == 2) {
            serveProviderService.settingStatus(currentUserId);
            settingsStatus = 1;

            //插入同步表
            ServeProviderSyncDO serveProviderSyncDO =
                    ServeProviderSyncDO.builder()
                            .id(currentUserId)
                            .settingStatus(settingsStatus)
                            .build();
            serveProviderSyncService.updateById(serveProviderSyncDO);
        }

        // 构造响应
        ServeSettingsStatusDTO serveSettingsStatusDTO = ServeSettingsStatusDTO.builder()
                .certificationStatus(certificationStatus)//认证状态
                .settingsStatus(settingsStatus)//首先设置状态是否完成
                .serveSkillSetted(serveSkillSetted)//是否设置服务技能
                .serveScopeSetted(serveScopeSetted)//是否设置服务范围
                .canPickUp(canPickUp)//开启接单状态
                .build();

        return serveSettingsStatusDTO;
    }

    @Override
    public ServeProviderSettingsDO findById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void setHaveSkill(Long currentUserId) {
        lambdaUpdate()
                .eq(ServeProviderSettingsDO::getId, currentUserId)
                .set(ServeProviderSettingsDO::getHaveSkill, 1)
                .update();
    }

    @Override
    public Map<Long, String> findManyCityCodeOfServeProvider(List<Long> ids) {
        if (CollUtils.isEmpty(ids)) {
            return new HashMap<>();
        }
        List<ServeProviderSettingsDO> serveProviderSettingDOS = baseMapper.batchQueryCityCodeByIds(ids);
        return CollUtils.isEmpty(serveProviderSettingDOS) ? new HashMap<>() :
                serveProviderSettingDOS.stream().collect(Collectors.toMap(ServeProviderSettingsDO::getId, ServeProviderSettingsDO::getCityCode));

    }


}
