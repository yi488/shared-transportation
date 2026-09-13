package com.example.shared_transportation.config;

import com.example.shared_transportation.entity.Station;
import com.example.shared_transportation.entity.User;
import com.example.shared_transportation.repository.StationRepository;
import com.example.shared_transportation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 启动时初始化：3 个珠海站点 + 默认管理员账号。 */
@Component
public class DataInitializer implements CommandLineRunner {

  private final StationRepository stationRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${admin.phone:13800000000}")
  private String adminPhone;

  @Value("${admin.password:admin123}")
  private String adminPassword;

  public DataInitializer(
      StationRepository stationRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.stationRepository = stationRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    initStations();
    initAdmin();
  }

  private void initStations() {
    if (stationRepository.count() > 0) {
      return;
    }
    stationRepository.save(station("ST-001", "日月贝停车场", "香洲区情侣路（珠海大剧院）", 113.5876, 22.2923));
    stationRepository.save(station("ST-002", "拱北口岸停车场", "香洲区拱北口岸", 113.5495, 22.2177));
    stationRepository.save(station("ST-003", "唐家湾站停车场", "高新区唐家湾轻轨站", 113.5830, 22.3690));
  }

  private void initAdmin() {
    if (userRepository.existsByRole("ADMIN")) {
      return;
    }
    User admin = new User();
    admin.setPhone(adminPhone);
    admin.setPasswordHash(passwordEncoder.encode(adminPassword));
    admin.setRole("ADMIN");
    admin.setNickname("管理员");
    userRepository.save(admin);
  }

  private Station station(String id, String name, String address, double lng, double lat) {
    Station station = new Station();
    station.setId(id);
    station.setName(name);
    station.setAddress(address);
    station.setLongitude(lng);
    station.setLatitude(lat);
    return station;
  }
}
