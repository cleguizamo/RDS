package com.rds.app_restaurante.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;
    private final String defaultFolder;

    public CloudinaryService(
        @Value("${cloudinary.cloud-name}") String cloudName,
        @Value("${cloudinary.api-key}") String apiKey,
        @Value("${cloudinary.api-secret}") String apiSecret,
        @Value("${cloudinary.folder:restaurante/productos}") String defaultFolder
    ) {
        // Validar que las credenciales no sean los valores por defecto
        if ("TU_CLOUD_NAME".equals(cloudName) || "root".equalsIgnoreCase(cloudName) || cloudName == null || cloudName.trim().isEmpty()) {
            throw new IllegalStateException(
                "Cloudinary no está configurado correctamente. " +
                "El cloud_name no puede ser 'root' (ese es el nombre de la clave API, no el cloud_name). " +
                "El cloud_name aparece en la parte superior del dashboard de Cloudinary (ej: 'dxz8y9abc'). " +
                "Por favor, configura las variables de entorno CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET, " +
                "o actualiza los valores en application.yml."
            );
        }
        
        if ("TU_API_KEY".equals(apiKey) || apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "Cloudinary API Key no está configurado. " +
                "Por favor, configura la variable de entorno CLOUDINARY_API_KEY o actualiza el valor en application.yml."
            );
        }
        
        if ("TU_API_SECRET".equals(apiSecret) || apiSecret == null || apiSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "Cloudinary API Secret no está configurado. " +
                "Haz clic en 'Reveal' junto a 'API Secret' en el dashboard de Cloudinary para verlo. " +
                "Por favor, configura la variable de entorno CLOUDINARY_API_SECRET o actualiza el valor en application.yml."
            );
        }
        
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret
        ));
        this.defaultFolder = defaultFolder;
        
        System.out.println("Cloudinary configurado correctamente para cloud: " + cloudName);
    }

    
    public Map upload(MultipartFile multipartFile) throws IOException {
        return upload(multipartFile, defaultFolder);
    }

    
    public Map upload(MultipartFile multipartFile, String folder) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IOException("El archivo está vacío o es nulo");
        }

        File file = null;
        try {
            file = convert(multipartFile);
            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                "folder", folder
            );
            Map uploadResult = cloudinary.uploader().upload(file, uploadOptions);
            
            if (uploadResult == null || uploadResult.isEmpty()) {
                throw new IOException("Error: Cloudinary no devolvió un resultado válido. Verifica tus credenciales de Cloudinary.");
            }
            
            return uploadResult;
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw e;
            }
            // Si es otra excepción (ej: de Cloudinary), envolverla en IOException
            throw new IOException("Error al subir imagen a Cloudinary: " + e.getMessage(), e);
        } finally {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    private File convert(MultipartFile multipartFile) throws IOException {
        String fileName = UUID.randomUUID() + "_" + multipartFile.getOriginalFilename();
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
        try (OutputStream os = new FileOutputStream(convFile)) {
            os.write(multipartFile.getBytes());
        }
        return convFile;
    }
}
