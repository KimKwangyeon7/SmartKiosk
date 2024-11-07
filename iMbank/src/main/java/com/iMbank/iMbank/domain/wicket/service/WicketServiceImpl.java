package com.iMbank.iMbank.domain.wicket.service;

import com.iMbank.iMbank.domain.counsel.repository.CounselRepository;
import com.iMbank.iMbank.domain.department.entity.Department;
import com.iMbank.iMbank.domain.department.repository.DepartmentRepository;
import com.iMbank.iMbank.domain.department.repository.WorkRepository;
import com.iMbank.iMbank.domain.kiosk.dto.KioskInfo;
import com.iMbank.iMbank.domain.kiosk.entity.Kiosk;
import com.iMbank.iMbank.domain.kiosk.repository.KioskRepository;
import com.iMbank.iMbank.domain.member.repository.MemberRepository;
import com.iMbank.iMbank.domain.wicket.dto.WicketListInfo;
import com.iMbank.iMbank.domain.wicket.dto.request.CreateWicketRequest;
import com.iMbank.iMbank.domain.wicket.dto.request.UpdatedWicketInfoList;
import com.iMbank.iMbank.domain.wicket.dto.response.MapLayoutResponse;
import com.iMbank.iMbank.domain.wicket.entity.Wicket;
import com.iMbank.iMbank.domain.wicket.repository.WicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WicketServiceImpl implements WicketService {

    private final WicketRepository wicketRepository;
    private final DepartmentRepository departmentRepository;
    private final KioskRepository kioskRepository;
    private final MemberRepository memberRepository;
    private final WorkRepository workRepository;
    private final CounselRepository counselRepository;

    @Override
    public MapLayoutResponse getWicketListInfo(String deptNm) {
        System.out.println(deptNm);
        Department dept = departmentRepository.findByDeptNM(deptNm).orElse(null);
        List<Wicket> wicketList = wicketRepository.findByDeptNm(dept);
        TreeSet<Integer> set = new TreeSet<>();
        Map<Integer, Map<String, String>> map = new HashMap<>();
        List<String> detail = new ArrayList<>();

        for (Wicket dto : wicketList) {
            String key = dto.getRowNum() + "," + dto.getColNum();
            String value = "창구 " + dto.getWd_num() + "," + dto.getWd_id();
            set.add(dto.getWd_floor());
            String userNm = memberRepository.findNameById(dto.getUser_id());
            String workDvcdNm = workRepository.findWorkDvcdNmByDepartmentAndWorkDvcd(dept.getDept_nm(), dto.getWd_dvcd());
            detail.add(dto.getRowNum() + " " + dto.getColNum() + " " + dto.getWd_id() + " " + dto.getWd_num() + " " +
                    userNm + " " + workDvcdNm);
            if (!map.containsKey(dto.getWd_floor())) {
                Map<String, String> tmp = new HashMap<>();
                tmp.put(key, value);
                map.put(dto.getWd_floor(), tmp);
            } else {
                Map<String, String> tmp = map.get(dto.getWd_floor());
                tmp.put(key, value);
                map.put(dto.getWd_floor(), tmp);
            }
        }
        WicketListInfo wicketListInfo = new WicketListInfo(map);

        Kiosk kiosk = kioskRepository.findByDepartment(dept).orElse(null);
        String[] str = new String[1];
        str[0] = kiosk.getRowNum() + "," + kiosk.getColNum();
        Map<Integer, String[]> kmap = new HashMap<>();
        kmap.put(1, str);
        KioskInfo kioskInfo = new KioskInfo(kmap);

        int[] floors = new int[set.size()];
        int cnt = 0;
        for (int i : set) {
            floors[cnt++] = i;
        }
        return new MapLayoutResponse(wicketListInfo, kioskInfo, floors, detail);
    }

    @Override
    public void sendUpdatedWicketListInfo(UpdatedWicketInfoList updatedWicketInfoList) {
        // 키오스크 위치 수정
        if (!updatedWicketInfoList.kiosks().isEmpty()) {
            for (Map<String, String> map : updatedWicketInfoList.kiosks()) {
                int x = Integer.parseInt(map.get("to").split(",")[0]);
                int y = Integer.parseInt(map.get("to").split(",")[1]);

                Kiosk kiosk = kioskRepository.findByDepartment(departmentRepository.findByDeptNM("강남").orElse(null)).orElse(null);
                if (kiosk != null) {
                    kiosk.setColNum(y);
                    kiosk.setRowNum(x);
                    kioskRepository.save(kiosk);
                }
            }
        }

        // 창구 위치 수정
        if (!updatedWicketInfoList.counters().isEmpty()) {
            for (Map<String, String> map : updatedWicketInfoList.counters()) {
                int x = Integer.parseInt(map.get("to").split(",")[0]);
                int y = Integer.parseInt(map.get("to").split(",")[1]);
                int counterNum = Integer.parseInt(map.get("counterName").split(",")[0].substring(3));
                System.out.println("counterNum: " + counterNum);
                int counterId = Integer.parseInt(map.get("counterName").split(",")[1]);
                System.out.println("counterId: " + counterId);
                Wicket wicket = wicketRepository.findByWd_id(counterId);
                wicket.setRowNum(x);
                wicket.setColNum(y);
                wicket.setWd_num(counterNum);

                wicketRepository.save(wicket);
            }
        }
    }

    @Override
    public int createWicket(CreateWicketRequest createWicketRequest) {
        Department deptId = departmentRepository.findByDeptNM(createWicketRequest.deptNm()).orElse(null);
        String wdDvcd = workRepository.findWorkDvcdByWorkDvcdNm(createWicketRequest.wdDvcdNm(), deptId);
        int wdNum = createWicketRequest.wdNum();
        int wdFloor = createWicketRequest.wdFloor();
        int colNum = 0;
        int rowNum = 0;
        int userId = 1;
        Wicket wicket = new Wicket(deptId, wdDvcd, wdNum, wdFloor, rowNum, colNum, userId);
        wicketRepository.save(wicket);
        return wicket.getWd_id();
    }

    @Override
    public void deleteWicket(int wdId) {
        Department dept = departmentRepository.findByDeptId("B001");
        System.out.println(wdId);
        counselRepository.deleteByWdId(wdId, "B001");
        wicketRepository.deleteByWdId(wdId);
    }
}
