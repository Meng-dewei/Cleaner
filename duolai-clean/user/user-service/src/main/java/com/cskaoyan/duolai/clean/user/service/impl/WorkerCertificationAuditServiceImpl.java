package com.cskaoyan.duolai.clean.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.user.converter.WorkerCertificationConverter;
import com.cskaoyan.duolai.clean.user.dao.entity.ServeProviderDO;
import com.cskaoyan.duolai.clean.user.dao.mapper.WorkerCertificationMapper;
import com.cskaoyan.duolai.clean.user.enums.CertificationAuditStatusEnum;
import com.cskaoyan.duolai.clean.user.enums.CertificationStatusEnum;
import com.cskaoyan.duolai.clean.user.dao.mapper.WorkerCertificationAuditMapper;
import com.cskaoyan.duolai.clean.user.dao.entity.WorkerCertificationDO;
import com.cskaoyan.duolai.clean.user.dao.entity.WorkerCertificationAuditDO;
import com.cskaoyan.duolai.clean.user.request.CertificationAuditCommand;
import com.cskaoyan.duolai.clean.user.request.WorkerCertificationAuditCommand;
import com.cskaoyan.duolai.clean.user.request.WorkerCertificationAuditPageRequest;
import com.cskaoyan.duolai.clean.user.dto.RejectReasonResDTO;
import com.cskaoyan.duolai.clean.user.dto.WorkerCertificationAuditDTO;
import com.cskaoyan.duolai.clean.user.service.IServeProviderService;
import com.cskaoyan.duolai.clean.user.service.IWorkerCertificationAuditService;
import com.cskaoyan.duolai.clean.user.service.IWorkerCertificationService;
import com.cskaoyan.duolai.clean.common.expcetions.BadRequestException;
import com.cskaoyan.duolai.clean.common.model.CurrentUserInfo;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import com.cskaoyan.duolai.clean.mysql.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 服务人员认证审核表 服务实现类
 * </p>
 * <p>
 * “”
 * "" 2023-09-06
 */
@Service
public class WorkerCertificationAuditServiceImpl extends ServiceImpl<WorkerCertificationAuditMapper, WorkerCertificationAuditDO> implements IWorkerCertificationAuditService {
    @Resource
    private IWorkerCertificationService workerCertificationService;
    @Resource
    private IServeProviderService serveProviderService;

    @Resource
    WorkerCertificationConverter workerCertificationConverter;


    @Autowired
    WorkerCertificationMapper workerCertificationMapper;

    /**
     * 服务人员申请资质认证
     *
     * @param workerCertificationAuditCommand 认证申请请求体
     */
    @Override
    @Transactional
    public void applyCertification(WorkerCertificationAuditCommand workerCertificationAuditCommand) {
        //查询认证记录(因为可能存在多条，只查询最新的一条)
        Long serveProviderId = workerCertificationAuditCommand.getServeProviderId();

        LambdaQueryWrapper<WorkerCertificationDO> workerCertificationWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<WorkerCertificationDO> last = workerCertificationWrapper
                .eq(WorkerCertificationDO::getServeProviderId, serveProviderId)
                .orderByDesc(WorkerCertificationDO::getCreateTime)
                .last("limit 1");
        WorkerCertificationDO workerCertificationDO = workerCertificationMapper.selectOne(last);
        if (ObjectUtil.isNotNull(workerCertificationDO)) {
            //2.将认证信息状态更新为认证中
            if (CertificationStatusEnum.PROGRESSING.getStatus() == workerCertificationDO.getCertificationStatus()) {
                throw new BadRequestException("认证信息正在审核中，请稍后再试");
            }
        }


        /*
             保存认证记录:
             1. 将workerCertificationAuditCommand转化为WorkerCertificationDO
             2. 在设置providerId，certificationStatus为CertificationStatusEnum.PROGRESSING.getStatus(), 以及设置认证时间
             3. 保存
         */
        WorkerCertificationDO newCertification = workerCertificationConverter.workCertificationAuditCommandToWorkCertificationDO(workerCertificationAuditCommand);
        newCertification.setServeProviderId(serveProviderId);
        newCertification.setCertificationStatus(CertificationStatusEnum.PROGRESSING.getStatus());
        newCertification.setCertificationTime(LocalDateTime.now());
        workerCertificationMapper.insert(newCertification);

        /*
             添加审核记录
             1. workerCertificationAuditCommand转化为WorkerCertificationAuditDO
             2. 设置审核状态为CertificationAuditStatusEnum.NOT_AUDIT.getStatus(),
                  设置认证状态为CertificationStatusEnum.PROGRESSING.getStatus()，设置serveProviderId
             3. 保存
         */
        WorkerCertificationAuditDO auditDO = workerCertificationConverter.workCertificationAuditCommandToDO(workerCertificationAuditCommand);
        auditDO.setServeProviderId(serveProviderId);
        auditDO.setAuditStatus(CertificationAuditStatusEnum.NOT_AUDIT.getStatus());
        auditDO.setCertificationStatus(CertificationStatusEnum.PROGRESSING.getStatus());
        auditDO.setCreateTime(LocalDateTime.now());
        auditDO.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(auditDO);
    }

    /**
     * 审核认证信息
     *
     * @param id                        审核记录id
     * @param certificationAuditCommand 审核请求
     */
    @Override
    @Transactional
    public void auditCertification(Long id, CertificationAuditCommand certificationAuditCommand) {

        //1. 获取用户信息(获取当前后台工作人言的信息)
        CurrentUserInfo currentUserInfo = UserContext.currentUser();


        //2.根据id更新审核记录work_certification_audit(审核状态，审核人，审核人姓名，审核时间，审核状态，如果审核失败(失败原因不空)，要保存审核失败原因)
        WorkerCertificationAuditDO auditRecord = baseMapper.selectById(id);
        if (auditRecord == null) {
            throw new BadRequestException("审核记录不存在");
        }
        auditRecord.setAuditStatus(CertificationAuditStatusEnum.AUDIT_FINISH.getStatus());
        auditRecord.setAuditorId(currentUserInfo.getId());
        auditRecord.setAuditorName(currentUserInfo.getName());
        auditRecord.setAuditTime(LocalDateTime.now());
        auditRecord.setCertificationStatus(certificationAuditCommand.getCertificationStatus());

        if (certificationAuditCommand.getCertificationStatus() == CertificationStatusEnum.FAIL.getStatus()) {
            auditRecord.setRejectReason(certificationAuditCommand.getRejectReason());
        }
        baseMapper.updateById(auditRecord);

        //3.更新认证信息(work_certification)主要是修改认证状态(certification_status), 如果认证成功，需要更新一下服务人员(serveProvider表)的姓名
        /*
             需要注意的是:
             1). 可以通过审核记录的id值，查询出审核记录，审核记录中有对应的服务人员的serveProviderId
             2). 同一个服务从业者，可能有多条认证申请记录，我们应该更新其最新的一条，根据createTime降序排序的第一条，然后更新
                LambdaQueryWrapper<WorkerCertificationDO> workCertificationDO = workerCertificationQueryWrapper
                       .eq(WorkerCertificationDO::getServeProviderId,workerCertificationAuditDO.getServeProviderId())
                .orderByDesc(WorkerCertificationDO::getCreateTime)
                .last("limit 1");
         */
        LambdaQueryWrapper<WorkerCertificationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkerCertificationDO::getServeProviderId, auditRecord.getServeProviderId())
                .orderByDesc(WorkerCertificationDO::getCreateTime)
                .last("limit 1");
        WorkerCertificationDO latestCertification = workerCertificationMapper.selectOne(queryWrapper);
        if (latestCertification != null) {
            latestCertification.setCertificationStatus(certificationAuditCommand.getCertificationStatus());
            latestCertification.setCertificationTime(LocalDateTime.now());
            workerCertificationMapper.updateById(latestCertification);

            if (certificationAuditCommand.getCertificationStatus() == CertificationStatusEnum.SUCCESS.getStatus()) {
                ServeProviderDO serveProvider = serveProviderService.getById(auditRecord.getServeProviderId());
                if(serveProvider != null){
                    serveProvider.setName(auditRecord.getName());
                    serveProviderService.updateById(serveProvider);
                }
            }
        }
    }

    /**
     * 分页查询
     *
     * @param workerCertificationAuditPageRequestDTO 分页查询条件
     * @return 分页结果
     */
    @Override
    public PageDTO<WorkerCertificationAuditDTO> pageQuery(WorkerCertificationAuditPageRequest workerCertificationAuditPageRequestDTO) {
        Page<WorkerCertificationAuditDO> page = PageUtils.parsePageQuery(workerCertificationAuditPageRequestDTO, WorkerCertificationAuditDO.class);

        LambdaQueryWrapper<WorkerCertificationAuditDO> queryWrapper = Wrappers.<WorkerCertificationAuditDO>lambdaQuery()
                .like(ObjectUtil.isNotEmpty(workerCertificationAuditPageRequestDTO.getName()), WorkerCertificationAuditDO::getName, workerCertificationAuditPageRequestDTO.getName())
                .eq(ObjectUtil.isNotEmpty(workerCertificationAuditPageRequestDTO.getIdCardNo()), WorkerCertificationAuditDO::getIdCardNo, workerCertificationAuditPageRequestDTO.getIdCardNo())
                .eq(ObjectUtil.isNotEmpty(workerCertificationAuditPageRequestDTO.getAuditStatus()), WorkerCertificationAuditDO::getAuditStatus, workerCertificationAuditPageRequestDTO.getAuditStatus())
                .eq(ObjectUtil.isNotEmpty(workerCertificationAuditPageRequestDTO.getCertificationStatus()), WorkerCertificationAuditDO::getCertificationStatus, workerCertificationAuditPageRequestDTO.getCertificationStatus());
        Page<WorkerCertificationAuditDO> result = baseMapper.selectPage(page, queryWrapper);

        return workerCertificationConverter.workerCertificationAuditDOsToPageDTO(result.getRecords(), result.getTotal(), result.getPages());
    }

    /**
     * 查询当前用户最近驳回原因
     *
     * @return 驳回原因
     */
    @Override
    public RejectReasonResDTO queryCurrentUserLastRejectReason() {
        // 根据用户id和审核记录的创建时间倒序排序，取第一条记录中的驳回原因
        LambdaQueryWrapper<WorkerCertificationAuditDO> queryWrapper = Wrappers.<WorkerCertificationAuditDO>lambdaQuery()
                .eq(WorkerCertificationAuditDO::getServeProviderId, UserContext.currentUserId())
                .orderByDesc(WorkerCertificationAuditDO::getCreateTime)
                .last("limit 1");
        WorkerCertificationAuditDO workerCertificationAuditDO = baseMapper.selectOne(queryWrapper);
        return new RejectReasonResDTO(workerCertificationAuditDO.getRejectReason());
    }
}
