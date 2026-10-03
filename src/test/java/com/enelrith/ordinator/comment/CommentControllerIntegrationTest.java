package com.enelrith.ordinator.comment;

import com.enelrith.ordinator.TestcontainersConfiguration;
import com.enelrith.ordinator.comment.dto.CommentDto;
import com.enelrith.ordinator.comment.dto.CreateCommentRequest;
import com.enelrith.ordinator.common.exception.NotFoundException;
import com.enelrith.ordinator.project.*;
import com.enelrith.ordinator.task.Task;
import com.enelrith.ordinator.task.TaskImportance;
import com.enelrith.ordinator.task.TaskRepository;
import com.enelrith.ordinator.task.TaskStatus;
import com.enelrith.ordinator.user.User;
import com.enelrith.ordinator.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test@email.com")
class CommentControllerIntegrationTest {
    private static final String COMMENTS_URI = "/api/comments";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private S3Client s3Client;

    User savedUser;
    Project savedProject;
    ProjectMember savedProjectMember;
    Task savedTask;

    @BeforeAll
    void createBucket() {
        s3Client.createBucket(b -> b.bucket("ordinator-attachments"));
    }

    @BeforeEach
    void setup() {
        var user = new User("test@email.com", "hashedPassword", "test", "test");
        savedUser = userRepository.save(user);

        var project = new Project("test", "test", ProjectStatus.ONGOING, savedUser);
        savedProject = projectRepository.save(project);

        var projectMember = new ProjectMember(ProjectMemberRole.ADMIN, savedProject, savedUser);
        savedProjectMember = projectMemberRepository.save(projectMember);

        var task = new Task("test", "test", TaskStatus.ONGOING, TaskImportance.CRITICAL, savedProjectMember);
        savedTask = taskRepository.save(task);

        task.addProjectMember(savedProjectMember);
    }

    @Test
    void createComment_withAttachment_returnsCreated() throws Exception {
        var commentRequest = buildTestCreateCommentRequest();
        var commentPart = buildTestCommentPart(commentRequest);
        var attachmentFilePart = buildTestAttachmentFilePart();

        var result = mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                        .with(csrf())
                        .file(commentPart)
                        .file(attachmentFilePart))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value(commentRequest.content()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.attachmentName").value("test.txt"))
                .andExpect(jsonPath("$.author.id").value(savedProjectMember.getId().toString()))
                .andExpect(jsonPath("$.author.role").value(savedProjectMember.getRole().toString()))
                .andExpect(jsonPath("$.author.user.id").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.author.user.email").value(savedUser.getEmail()))
                .andExpect(jsonPath("$.author.user.firstName").value(savedUser.getFirstName()))
                .andExpect(jsonPath("$.author.user.lastName").value(savedUser.getLastName()))
                .andReturn();

        var content = result.getResponse().getContentAsString();
        var commentDto = jsonMapper.readValue(content, CommentDto.class);
        var commentId = commentDto.id();

        var savedComment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        var savedAttachmentObjectKey = savedComment.getAttachmentObjectKey();

        var attachmentBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket("ordinator-attachments")
                .key(savedAttachmentObjectKey)
                .build());

        assertEquals("testing file", attachmentBytes.asUtf8String());
    }

    @Test
    void createComment_withoutAttachment_returnsCreated() throws Exception {
        var commentRequest = buildTestCreateCommentRequest();
        var commentPart = buildTestCommentPart(commentRequest);

        mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                .with(csrf())
                .file(commentPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.attachmentName").isEmpty())
                .andExpect(jsonPath("$.author.id").value(savedProjectMember.getId().toString()))
                .andExpect(jsonPath("$.author.user.id").value(savedUser.getId().toString()));
    }

    @Test
    @WithMockUser(username = "another.user@email.com")
    void createComment_withProjectMemberNotATaskMember_returnsNotFound() throws Exception {
        var anotherUser = new User("another.user@email.com", "hashedPassword", "test", "test");
        userRepository.save(anotherUser);

        var anotherProjectMember = new ProjectMember(ProjectMemberRole.MANAGER, savedProject, anotherUser);
        projectMemberRepository.save(anotherProjectMember);

        var commentRequest = buildTestCreateCommentRequest();
        var commentPart = buildTestCommentPart(commentRequest);

        mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                        .with(csrf())
                        .file(commentPart))
                .andExpect(status().isNotFound());
    }

    static Stream<String> invalidFileNames() {
        return Stream.of("", "   ", "a".repeat(256));
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void createComment_withInvalidAttachmentFileName_returnsBadRequest(String fileName) throws Exception {
        var commentRequest = buildTestCreateCommentRequest();
        var commentPart = buildTestCommentPart(commentRequest);
        var attachmentFilePart = buildTestAttachmentFilePart(fileName);

        mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                        .with(csrf())
                        .file(commentPart)
                        .file(attachmentFilePart))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createComment_withInvalidCommentRequest_returnsBadRequest() throws Exception {
        var commentRequest = new CreateCommentRequest("  ");
        var commentPart = buildTestCommentPart(commentRequest);

        mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                        .with(csrf())
                        .file(commentPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void createComment_withUserNotAuthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createComment_withMissingCsrfToken_returnsForbidden() throws Exception {
        mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllTaskComments_returnsOkAndPaginationWorks() throws Exception {
        int commentNumber = 20;
        UUID firstCommentId = null;
        UUID lastCommentId = null;
        for (int i = 0; i < commentNumber; i++) {
            var commentRequest = buildTestCreateCommentRequest();
            var comment = CommentMapper.toEntity(commentRequest, null, null, savedTask, savedProjectMember);
            var savedComment = commentRepository.save(comment);
            if (i == 0) {
                firstCommentId = savedComment.getId();
            } else if (i == commentNumber - 1) {
                lastCommentId = savedComment.getId();
            }
        }

        assertNotNull(firstCommentId);
        assertNotNull(lastCommentId);

        mockMvc.perform(get(COMMENTS_URI + "/projects/" + savedProject.getId() + "/tasks/" + savedTask.getId() + "?page=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.content[0].id").value(lastCommentId.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(commentNumber));

        mockMvc.perform(get(COMMENTS_URI + "/projects/" + savedProject.getId() + "/tasks/" + savedTask.getId() + "?page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.content[9].id").value(firstCommentId.toString()));
    }

    @Test
    @WithMockUser(username = "another.user@email.com")
    void getAllTaskComments_withUserNotTaskMember_returnsOk() throws Exception {
        var anotherUser = new User("another.user@email.com", "hashedPassword", "test", "test");
        userRepository.save(anotherUser);

        var anotherProjectMember = new ProjectMember(ProjectMemberRole.MANAGER, savedProject, anotherUser);
        projectMemberRepository.save(anotherProjectMember);

        var commentRequest = buildTestCreateCommentRequest();
        var comment = CommentMapper.toEntity(commentRequest, null, null, savedTask, savedProjectMember);
        var savedComment = commentRepository.save(comment);

        mockMvc.perform(get(COMMENTS_URI + "/projects/" + savedProject.getId() + "/tasks/" + savedTask.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(savedComment.getId().toString()));
    }

    @Test
    @WithMockUser(username = "another.user@email.com")
    void getAllTaskComments_withUserNotProjectMembers_returnsNotFound() throws Exception {
        var anotherUser = new User("another.user@email.com", "hashedPassword", "test", "test");
        userRepository.save(anotherUser);

        var commentRequest = buildTestCreateCommentRequest();
        var comment = CommentMapper.toEntity(commentRequest, null, null, savedTask, savedProjectMember);
        commentRepository.save(comment);

        mockMvc.perform(get(COMMENTS_URI + "/projects/" + savedProject.getId() + "/tasks/" + savedTask.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test
    @WithAnonymousUser
    void getAllTaskComments_withUnauthenticatedUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(COMMENTS_URI + "/projects/" + savedProject.getId() + "/tasks/" + savedTask.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void downloadAttachment_returnsOk() throws Exception {
        var commentRequest = buildTestCreateCommentRequest();
        var commentPart = buildTestCommentPart(commentRequest);
        var attachmentFilePart = buildTestAttachmentFilePart();

        var createdCommentResult = mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                        .with(csrf())
                        .file(commentPart)
                        .file(attachmentFilePart))
                .andExpect(status().isCreated())
                .andReturn();

        var content = createdCommentResult.getResponse().getContentAsString();
        var commentDto = jsonMapper.readValue(content, CommentDto.class);
        var commentId = commentDto.id();

        var downloadAttachmentResult = mockMvc.perform(get(COMMENTS_URI + "/" + commentId + "/attachment"))
                .andExpect(status().isOk())
                .andReturn();

        var fileContent = downloadAttachmentResult.getResponse().getContentAsString();

        assertEquals("testing file", fileContent);
    }

    @Test
    void downloadAttachment_withMissingAttachment_returnsNotFound() throws Exception {
        var commentRequest = buildTestCreateCommentRequest();
        var commentPart = buildTestCommentPart(commentRequest);

        var createdCommentResult = mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                        .with(csrf())
                        .file(commentPart))
                .andExpect(status().isCreated())
                .andReturn();

        var content = createdCommentResult.getResponse().getContentAsString();
        var commentDto = jsonMapper.readValue(content, CommentDto.class);
        var commentId = commentDto.id();

        mockMvc.perform(get(COMMENTS_URI + "/" + commentId + "/attachment"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadAttachment_withProjectMemberNotTaskMember_returnsNotFound() throws Exception {
        var anotherUser = new User("another.user@email.com", "hashedPassword", "test", "test");
        userRepository.save(anotherUser);

        var anotherProjectMember = new ProjectMember(ProjectMemberRole.MANAGER, savedProject, anotherUser);
        projectMemberRepository.save(anotherProjectMember);

        var commentRequest = buildTestCreateCommentRequest();
        var commentPart = buildTestCommentPart(commentRequest);
        var attachmentFilePart = buildTestAttachmentFilePart();

        var createdCommentResult = mockMvc.perform(multipart(COMMENTS_URI + "/tasks/" + savedTask.getId())
                        .with(csrf())
                        .file(commentPart)
                        .file(attachmentFilePart))
                .andExpect(status().isCreated())
                .andReturn();

        var content = createdCommentResult.getResponse().getContentAsString();
        var commentDto = jsonMapper.readValue(content, CommentDto.class);
        var commentId = commentDto.id();

        mockMvc.perform(get(COMMENTS_URI + "/" + commentId + "/attachment"))
                .andExpect(status().isOk());

        mockMvc.perform(get(COMMENTS_URI + "/" + commentId + "/attachment")
                        .with(user(anotherUser.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void downloadAttachment_withUnauthenticatedUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(COMMENTS_URI + "/" + UUID.randomUUID() + "/attachment"))
                .andExpect(status().isUnauthorized());
    }

    private CreateCommentRequest buildTestCreateCommentRequest() {
        return new CreateCommentRequest("test content");
    }

    private MockMultipartFile buildTestCommentPart(CreateCommentRequest commentRequest) {
        return new MockMultipartFile(
                "commentRequest", "",
                MediaType.APPLICATION_JSON_VALUE,
                jsonMapper.writeValueAsBytes(commentRequest)
        );
    }

    private MockMultipartFile buildTestAttachmentFilePart() {
        return new MockMultipartFile(
                "attachmentFile", "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "testing file".getBytes()
        );
    }

    private MockMultipartFile buildTestAttachmentFilePart(String fileName) {
        return new MockMultipartFile(
                "attachmentFile", fileName,
                MediaType.TEXT_PLAIN_VALUE,
                "testing file".getBytes()
        );
    }
}
