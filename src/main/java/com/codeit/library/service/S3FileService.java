package com.codeit.library.service;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static software.amazon.awssdk.utils.StringUtils.substring;

@Service
@RequiredArgsConstructor
public class S3FileService {

    // S3 버킷을 제어하는 객체
    private S3Client s3Client;

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;
    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;
    @Value("${spring.cloud.aws.region.static}")
    private String region;

    // S3에 연결해서 인증을 처리하는 로직
    @PostConstruct // 클래스를 기반으로 객체가 생성될 때 자동 실행되는 어노테이션
    private void initializeAmazonS3Client() {

        // 액세스 키와 시크릿 키를 이용해서 계정 인증 받기
        AwsBasicCredentials credentials
                = AwsBasicCredentials.create(accessKey, secretKey);// 발급받은 키 전달해서 처리

        // 지역 설정 및 인증 정보를 담은 S3Client 객체를 위의 변수 세팅
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    public String uploadToS3Bucket(MultipartFile file) throws IOException {

        // 1. 고유한 파일명 생성 (UUID + 원본 파일명)
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        // 2. S3에 업로드할 요청 객체 생성
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName) // 버킷 이름
                .key(uniqueFileName) // 저장될 파일명
                .contentType(file.getContentType())
                .build();

        // 3. 실제 S3에 파일 업로드
        s3Client.putObject(
                request,
//                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
                RequestBody.fromBytes(file.getBytes())
        );

        // 4. 업로드된 파일의 URL 반환
        // utilities()을 통해 버킷 이름과 파일명이 결합된 URL을 쉽게 얻을 수 있다 -> DB에 저장
        return s3Client.utilities()
                .getUrl(b -> b.bucket(bucketName).key(uniqueFileName))
                .toString();
    }

    // 특정 폴더에 파일을 업로드 (실제로는 폴더가 아니고, prefix로 파일을 구분)
    // ex : "users/profile/" -> users/profile/uuid_filename.jpg
    public String uploadFileToFolder(MultipartFile file, String folder) throws IOException {

        // 1. 고유한 파일명 생성 (UUID + 원본 파일명)
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = folder + UUID.randomUUID() + "_" + originalFileName;

        // 2. S3에 업로드할 요청 객체 생성
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName) // 버킷 이름
                .key(uniqueFileName) // 저장될 파일명
                .contentType(file.getContentType())
                .build();

        // 3. 실제 S3에 파일 업로드
        s3Client.putObject(
                request,
//                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
                RequestBody.fromBytes(file.getBytes())
        );

        // 4. 업로드된 파일의 URL 반환
        // utilities()을 통해 버킷 이름과 파일명이 결합된 URL을 쉽게 얻을 수 있다 -> DB에 저장
        return s3Client.utilities()
                .getUrl(b -> b.bucket(bucketName).key(uniqueFileName))
                .toString();

    }

    // 파일 객체 삭제
    // 버킷에 객체를 지우기 위해선 키값(파일명)을 줘야하는데
    // 우리가 DB에 저장해서 갖고 있는건 url이기 때문에 -> 정제를 해서 S3에 전달해야한다.
    // 여러 개 파일이면 반복문으로 처리??

    // 우리가 가진 데이터: https://s3-bucket-practice8917.s3.ap-northeast-2.amazonaws.com/74b59c79-d5da-4d05-b99a-557f00b4da07_fileName.gif
    // 가공 결과: 74b59c79-d5da-4d05-b99a-557f00b4da07_fileName.gif
    public void deleteFile(String imageUrl) throws Exception {

        String key = extractFileNameFromUrl(imageUrl);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName) // 버킷 이름
                .key(key) // 지우고자 하는 키 값
                .build();

        s3Client.deleteObject(request);
    }

    // 여러 파일 일괄 삭제
    public void deleteFiles(List<String> imageUrls) throws Exception {

        List<String> fileNames = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            String fileName = extractFileNameFromUrl(imageUrl);
            fileNames.add(fileName);
        }

        // 삭제할 객체 목록 생성
        List<ObjectIdentifier> objectIdentifiers = fileNames.stream()
                .map(fileName -> ObjectIdentifier.builder()
                        .key(fileName)
                        .build())
                .collect(Collectors.toList());

        // Delete 객체 생성
        Delete delete = Delete.builder()
                .objects(objectIdentifiers)
                .build();

        // 객체 생성 및 메서드 명 조심! (s 복수가 달려있음)
        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                .bucket(bucketName) // 버킷 이름
                .delete(delete)
                .build();

        s3Client.deleteObjects(request);
    }

    // 파일 다운로드 요청
    public byte[] downloadFile(String fileUrl) throws MalformedURLException, UnsupportedEncodingException {
        String fileName = extractFileNameFromUrl(fileUrl);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        ResponseBytes<GetObjectResponse> objectAsBytes =
                s3Client.getObjectAsBytes(getObjectRequest);

        return objectAsBytes.asByteArray();
    }

    // 파일 존재 여부 확인
    public boolean isFileExist(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);

            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            // headObject의 결과로 응답된 객체에서 데이터의 여러 정보를 얻을 수 있다.
            HeadObjectResponse response = s3Client.headObject(request);
//            response.metadata();

            return true;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (S3Exception e) {
            // 요청 보낸 객체(데이터)가 조회 되지 않으면 S3Exception이 발생
            // statusCode를 확인했을 때 404라면 존재하지 않은 파일
            if(e.statusCode() == 404) {
                return false;
            }
            throw new RuntimeException("파일 존재 요청 실패" + e.getMessage());
        }
    }

    // url에서 파일명만 추출
    private static String extractFileNameFromUrl(String imageUrl) throws MalformedURLException, UnsupportedEncodingException {
        URL url = new URL(imageUrl);

        // getPath() -> 프로토콜, ip(도메인), 포트번호를 제외한 리소스 내부 경로만 받음
        String decodeUrl = URLDecoder.decode(url.getPath(), "UTF-8");

        // 맨 앞에 있는 "/" 떼기 위해서 substring을 적용
        return decodeUrl.substring(1);
    }
}
