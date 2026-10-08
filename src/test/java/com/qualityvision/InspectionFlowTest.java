package com.qualityvision;

import com.qualityvision.domain.InspectionRecord;
import com.qualityvision.domain.Product;
import com.qualityvision.dto.DefectReport;
import com.qualityvision.mapper.ProductMapper;
import com.qualityvision.service.AuthService;
import com.qualityvision.service.InspectionService;
import com.qualityvision.service.MockVisionInspectService;
import com.qualityvision.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心流程集成测试（H2）：上传检测 → 落库 → 复核确认（幂等）→ 看板统计
 */
@SpringBootTest
@Sql("/schema-test.sql")
class InspectionFlowTest {

    @Autowired
    private InspectionService inspectionService;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private MockVisionInspectService mockService;
    @Autowired
    private AuthService authService;
    @Autowired
    private StatsService statsService;
    @Autowired
    private com.qualityvision.mapper.SysUserMapper userMapper;
    @Autowired
    private com.qualityvision.mapper.DefectDetailMapper defectDetailMapper;

    private byte[] testImage(int seed) throws Exception {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                img.setRGB(x, y, (x * 31 + y * 17 + seed * 101) & 0xFFFFFF);
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", bos);
        return bos.toByteArray();
    }

    @Test
    void mockIsDeterministic() throws Exception {
        byte[] img = testImage(7);
        DefectReport a = mockService.inspect(img, "手套");
        DefectReport b = mockService.inspect(img, "手套");
        assertEquals(a.isHasDefect(), b.isHasDefect());
        assertEquals(a.getDefects().size(), b.getDefects().size());
        assertEquals("MOCK", a.getSource());
    }

    @Test
    void inspectConfirmFlow() throws Exception {
        Product p = new Product();
        p.setCode("T-001");
        p.setName("测试产品");
        productMapper.insert(p);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg",
                "image/jpeg", testImage(3));
        InspectionRecord record = inspectionService.inspect(file, p.getId(), "B20261007");

        assertNotNull(record.getId());
        assertEquals("AI_DONE", record.getStatus());
        assertEquals("MOCK", record.getSource());
        assertNotNull(record.getAiResultJson());
        assertTrue(record.getImagePath().startsWith("uploads/"));

        // 缺陷明细同事务写入
        long detailCount = defectDetailMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.qualityvision.domain.DefectDetail>()
                        .eq(com.qualityvision.domain.DefectDetail::getRecordId, record.getId()));
        assertEquals(record.getDefectCount(), (int) detailCount);

        // 复核确认
        InspectionRecord confirmed = inspectionService.confirm(
                record.getId(), "FAIL", "人工复核：确有污点", "admin");
        assertEquals("CONFIRMED", confirmed.getStatus());
        assertEquals("FAIL", confirmed.getHumanVerdict());
        assertNotNull(confirmed.getConfirmedAt());

        // 幂等：重复确认直接返回，不抛错不改时间
        InspectionRecord again = inspectionService.confirm(
                record.getId(), "PASS", "改主意", "admin");
        assertEquals("FAIL", again.getHumanVerdict());
    }

    @Test
    void confirmMissingRecordThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> inspectionService.confirm(999999L, "PASS", "", "admin"));
    }

    @Test
    void authLogin() {
        com.qualityvision.domain.SysUser u = new com.qualityvision.domain.SysUser();
        String salt = AuthService.newSalt();
        u.setUsername("tester");
        u.setSalt(salt);
        u.setPasswordHash(AuthService.hash(salt, "pw123"));
        userMapper.insert(u);

        assertEquals("tester", authService.login("tester", "pw123").getUsername());
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("tester", "wrong"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("nobody", "pw123"));
    }

    @Test
    void dashboardStructure() {
        Map<String, Object> dash = statsService.dashboard(14);
        assertTrue(dash.containsKey("trend"));
        assertTrue(dash.containsKey("passRate"));
        assertTrue(dash.containsKey("defectPareto"));
        assertTrue(dash.containsKey("severityDist"));
        assertTrue(dash.containsKey("byProduct"));
        assertTrue(dash.containsKey("pending"));
    }
}
