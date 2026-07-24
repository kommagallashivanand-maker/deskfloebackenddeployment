package com.p99soft.deskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response details for a ticket attachment")
public class AttachmentResponse {

    @Schema(description = "Unique identifier of the attachment", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private UUID id;

    @Schema(description = "Original filename of the attachment", example = "error-logs.txt")
    private String fileName;

    @Schema(description = "The access URL of the stored file", example = "https://deskflow-attachments.s3.amazonaws.com/9b1deb4d-error-logs.txt")
    private String fileUrl;

    @Schema(description = "The MIME type of the file", example = "text/plain")
    private String fileType;

    @Schema(description = "The size of the file in bytes", example = "1048576")
    private Long fileSize;

    @Schema(description = "The timestamp when the attachment was uploaded")
    private LocalDateTime createdAt;
}
