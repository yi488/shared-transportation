package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.MyVehicleView;
import com.example.shared_transportation.dto.VehicleUpdateRequest;
import com.example.shared_transportation.dto.VehicleView;
import com.example.shared_transportation.entity.Vehicle;
import com.example.shared_transportation.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<VehicleView> listAvailable(String category, String stationId) {
        List<Vehicle> vehicles;
        if (stationId != null && !stationId.isBlank()) {
            vehicles = vehicleRepository
                    .findByStatusAndStationIdOrderByCreatedAtDesc("available", stationId);
        } else if (category != null && !category.isBlank()) {
            vehicles = vehicleRepository
                    .findByStatusAndCategoryOrderByCreatedAtDesc("available", category);
        } else {
            vehicles = vehicleRepository.findByStatusOrderByCreatedAtDesc("available");
        }
        return vehicles.stream()
                .map(v -> new VehicleView(v.getId(), v.getName(), v.getCategory(),
                        v.getImageUrl(), v.getStationId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MyVehicleView detail(String id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40400, "车辆不存在"));
        return toMyVehicleView(vehicle);
    }

    @Transactional(readOnly = true)
    public List<MyVehicleView> listMine(Long userId) {
        return vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toMyVehicleView)
                .toList();
    }

    @Transactional
    public MyVehicleView create(Long userId, MultipartFile file, String name, String desc,
                                String category, String stationId) {
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = storeImage(file);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setId(generateVehicleId());
        vehicle.setOwnerId(userId);
        vehicle.setName(name == null || name.isBlank() ? "我的车辆" : name.trim());
        vehicle.setCategory(normalizeCategory(category));
        vehicle.setDescription(desc == null ? "" : desc.trim());
        vehicle.setImageUrl(imageUrl);
        vehicle.setStationId(stationId);
        vehicle.setStatus("available");
        vehicleRepository.save(vehicle);
        return toMyVehicleView(vehicle);
    }

    @Transactional
    public MyVehicleView update(Long userId, String id, VehicleUpdateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40400, "车辆不存在"));
        if (!vehicle.getOwnerId().equals(userId)) {
            throw new BusinessException(40300, "无权操作该车辆");
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            vehicle.setName(request.getName().trim());
        }
        if (request.getDesc() != null) {
            vehicle.setDescription(request.getDesc().trim());
        }
        vehicleRepository.save(vehicle);
        return toMyVehicleView(vehicle);
    }

    @Transactional
    public void delete(Long userId, String id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40400, "车辆不存在"));
        if (!vehicle.getOwnerId().equals(userId)) {
            throw new BusinessException(40300, "无权操作该车辆");
        }
        if ("rented_out".equals(vehicle.getStatus())) {
            throw new BusinessException(40900, "车辆租出中，无法删除");
        }
        vehicleRepository.delete(vehicle);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "car";
        }
        String value = category.trim();
        return switch (value) {
            case "bike", "ebike", "car" -> value;
            default -> "car";
        };
    }

    private String generateVehicleId() {
        return "M-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String storeImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(40000, "请上传图片文件");
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new BusinessException(50001, "图片保存失败");
        }
    }

    private MyVehicleView toMyVehicleView(Vehicle v) {
        return new MyVehicleView(v.getId(), v.getName(), v.getDescription(),
                v.getImageUrl(), v.getStatus(), v.getStationId());
    }
}
