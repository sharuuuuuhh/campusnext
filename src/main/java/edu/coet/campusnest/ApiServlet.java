package edu.coet.campusnest;

import edu.coet.campusnest.dao.FeedbackDao;
import edu.coet.campusnest.dao.InquiryDao;
import edu.coet.campusnest.dao.ListingDao;
import edu.coet.campusnest.dao.OwnerDao;
import edu.coet.campusnest.dao.UserDao;
import edu.coet.campusnest.dao.impl.*;
import edu.coet.campusnest.exception.*;
import edu.coet.campusnest.model.*;
import edu.coet.campusnest.model.enums.*;
import edu.coet.campusnest.service.*;
import edu.coet.campusnest.service.impl.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/**
 * Main Controller Servlet for the CampusNest Web Application.
 * Demonstrates:
 * - Clean Separation of Concerns (Controller layer delegating to Service layer)
 * - Java Exception Handling (Translating custom OOP exceptions into HTTP status codes)
 * - Session Management and CSRF Security
 * - Polymorphic data dispatching
 */
@WebServlet("/api/*")
@MultipartConfig(maxFileSize = 5_242_880, maxRequestSize = 6_291_456, fileSizeThreshold = 1_048_576)
public class ApiServlet extends HttpServlet {

    private AuthService authService;
    private ListingService listingService;
    private InquiryService inquiryService;
    private FeedbackService feedbackService;
    private AdminService adminService;

    @Override
    public void init() throws ServletException {
        super.init();
        // Dependency Injection of DAO and Service layers
        UserDao userDao = new UserDaoImpl();
        OwnerDao ownerDao = new OwnerDaoImpl();
        ListingDao listingDao = new ListingDaoImpl();
        InquiryDao inquiryDao = new InquiryDaoImpl();
        FeedbackDao feedbackDao = new FeedbackDaoImpl();

        this.authService = new AuthServiceImpl(userDao, ownerDao);
        this.listingService = new ListingServiceImpl(listingDao);
        this.inquiryService = new InquiryServiceImpl(inquiryDao);
        this.feedbackService = new FeedbackServiceImpl(feedbackDao);
        this.adminService = new AdminServiceImpl(ownerDao, listingDao, inquiryDao, feedbackDao);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        try {
            super.service(req, resp);
        } catch (Exception ex) {
            getServletContext().log("Unexpected error processing API request", ex);
            if (!resp.isCommitted()) {
                Json.send(resp, 500, Json.error("An unexpected server error occurred: " + ex.getMessage()));
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = path(req);
        try {
            if (path.equals("/health")) {
                handleHealthCheck(resp);
                return;
            }
            if (path.equals("/auth/csrf")) {
                Json.send(resp, 200, Map.of("csrfToken", csrf(req.getSession(true))));
                return;
            }
            if (path.equals("/auth/me")) {
                Map<String, Object> me = new LinkedHashMap<>();
                me.put("user", sessionUser(req));
                me.put("csrfToken", csrf(req.getSession(true)));
                Json.send(resp, 200, me);
                return;
            }
            if (path.equals("/listings")) {
                ListingFilterCriteria criteria = buildCriteriaFromRequest(req, false);
                List<PgListing> listings = listingService.searchListings(criteria);
                List<Map<String, Object>> responseList = listings.stream().map(l -> l.toMap(false)).toList();
                Json.send(resp, 200, responseList);
                return;
            }
            if (path.matches("/listings/\\d+")) {
                long id = Long.parseLong(path.substring(10));
                PgListing listing = listingService.getListingById(id, false);
                Json.send(resp, 200, listing.toMap(false));
                return;
            }
            if (path.equals("/owner/listings")) {
                if (!requireRole(req, resp, "OWNER")) return;
                long ownerId = getSessionOwnerId(req);
                List<PgListing> listings = listingService.getOwnerListings(ownerId);
                List<Map<String, Object>> responseList = listings.stream().map(l -> l.toMap(true)).toList();
                Json.send(resp, 200, responseList);
                return;
            }
            if (path.equals("/owner/inquiries")) {
                if (!requireRole(req, resp, "OWNER")) return;
                long ownerId = getSessionOwnerId(req);
                List<Inquiry> inquiries = inquiryService.getInquiriesForOwner(ownerId);
                List<Map<String, Object>> responseList = inquiries.stream().map(Inquiry::toMap).toList();
                Json.send(resp, 200, responseList);
                return;
            }
            if (path.equals("/admin/dashboard")) {
                if (!requireRole(req, resp, "ADMIN")) return;
                Json.send(resp, 200, adminService.getDashboardStats());
                return;
            }
            if (path.equals("/admin/owners")) {
                if (!requireRole(req, resp, "ADMIN")) return;
                List<OwnerUser> queue = adminService.getOwnerQueue();
                List<Map<String, Object>> responseList = new ArrayList<>();
                for (OwnerUser o : queue) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.getOwnerId());
                    m.put("name", o.getName());
                    m.put("email", o.getEmail());
                    m.put("phone", o.getPhoneNo());
                    m.put("status", o.getOwnerStatus().name());
                    m.put("date", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                    m.put("proof", o.getProofOriginalName());
                    responseList.add(m);
                }
                Json.send(resp, 200, responseList);
                return;
            }
            if (path.matches("/admin/owners/\\d+/proof")) {
                if (!requireRole(req, resp, "ADMIN")) return;
                long ownerId = Long.parseLong(path.split("/")[3]);
                handleOwnerProofDownload(req, resp, ownerId);
                return;
            }
            if (path.equals("/admin/listings")) {
                if (!requireRole(req, resp, "ADMIN")) return;
                ListingFilterCriteria criteria = buildCriteriaFromRequest(req, true);
                List<PgListing> listings = listingService.searchListings(criteria);
                List<Map<String, Object>> responseList = listings.stream().map(l -> l.toMap(true)).toList();
                Json.send(resp, 200, responseList);
                return;
            }
            if (path.equals("/admin/feedback")) {
                if (!requireRole(req, resp, "ADMIN")) return;
                List<Feedback> feedbacks = feedbackService.getAllFeedback();
                List<Map<String, Object>> responseList = feedbacks.stream().map(Feedback::toMap).toList();
                Json.send(resp, 200, responseList);
                return;
            }
            Json.send(resp, 404, Json.error("Endpoint not found"));
        } catch (CampusNestException e) {
            Json.send(resp, e.getStatusCode(), Json.error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = path(req);
        if (!isCsrfValid(req, resp)) return;

        try {
            if (path.equals("/auth/register")) {
                handleStudentRegister(req, resp);
                return;
            }
            if (path.equals("/auth/login")) {
                handleLogin(req, resp);
                return;
            }
            if (path.equals("/auth/logout")) {
                HttpSession session = req.getSession(false);
                if (session != null) session.invalidate();
                Json.send(resp, 200, Map.of("ok", true));
                return;
            }
            if (path.equals("/owners/register")) {
                handleOwnerRegister(req, resp);
                return;
            }
            if (path.equals("/inquiries")) {
                handleCreateInquiry(req, resp);
                return;
            }
            if (path.equals("/owner/listings")) {
                if (!requireRole(req, resp, "OWNER")) return;
                handleCreateListing(req, resp);
                return;
            }
            if (path.equals("/feedback")) {
                handleCreateFeedback(req, resp);
                return;
            }
            Json.send(resp, 404, Json.error("Endpoint not found"));
        } catch (CampusNestException e) {
            Json.send(resp, e.getStatusCode(), Json.error(e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = path(req);
        if (!isCsrfValid(req, resp)) return;

        try {
            if (path.matches("/owner/listings/\\d+")) {
                if (!requireRole(req, resp, "OWNER")) return;
                long listingId = Long.parseLong(path.split("/")[3]);
                handleUpdateListing(req, resp, listingId);
                return;
            }
            if (path.matches("/owner/listings/\\d+/vacancy")) {
                if (!requireRole(req, resp, "OWNER")) return;
                long listingId = Long.parseLong(path.split("/")[3]);
                handleUpdateVacancy(req, resp, listingId);
                return;
            }
            if (path.matches("/admin/owners/\\d+/status")) {
                if (!requireRole(req, resp, "ADMIN")) return;
                long ownerId = Long.parseLong(path.split("/")[3]);
                handleUpdateOwnerStatus(req, resp, ownerId);
                return;
            }
            if (path.matches("/admin/listings/\\d+/status")) {
                if (!requireRole(req, resp, "ADMIN")) return;
                long listingId = Long.parseLong(path.split("/")[3]);
                handleUpdateListingModeration(req, resp, listingId);
                return;
            }
            if (path.matches("/admin/listings/\\d+")) {
                if (!requireRole(req, resp, "ADMIN")) return;
                long listingId = Long.parseLong(path.split("/")[3]);
                handleToggleListingVisibility(req, resp, listingId);
                return;
            }
            Json.send(resp, 404, Json.error("Endpoint not found"));
        } catch (CampusNestException e) {
            Json.send(resp, e.getStatusCode(), Json.error(e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = path(req);
        if (!isCsrfValid(req, resp)) return;
        if (!requireRole(req, resp, "ADMIN")) return;

        if (!path.matches("/admin/listings/\\d+")) {
            Json.send(resp, 404, Json.error("Endpoint not found"));
            return;
        }
        try {
            long listingId = Long.parseLong(path.split("/")[3]);
            listingService.deleteListing(listingId);
            Json.send(resp, 200, Map.of("ok", true));
        } catch (CampusNestException e) {
            Json.send(resp, e.getStatusCode(), Json.error(e.getMessage()));
        }
    }

    // Handlers
    private void handleHealthCheck(HttpServletResponse resp) throws IOException {
        boolean alive = false;
        try (java.sql.Connection c = Db.connect()) {
            alive = c.isValid(2);
        } catch (Exception ignored) {}
        Json.send(resp, alive ? 200 : 503, Map.of("status", alive ? "ok" : "unavailable", "database", alive));
    }

    private void handleStudentRegister(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        String name = stringVal(body, "name");
        String email = stringVal(body, "email");
        String password = stringVal(body, "password");
        String phone = stringVal(body, "phone");

        StudentUser student = authService.registerStudent(name, email, phone, password);
        Map<String, Object> sessionMap = student.toSessionMap();
        req.changeSessionId();
        req.getSession(true).setAttribute("user", sessionMap);
        Json.send(resp, 201, Map.of("user", sessionMap, "csrfToken", csrf(req.getSession(false))));
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        String email = stringVal(body, "email");
        String password = stringVal(body, "password");

        User user = authService.authenticate(email, password);
        Map<String, Object> sessionMap = user.toSessionMap();
        req.changeSessionId();
        HttpSession session = req.getSession(true);
        session.setAttribute("user", sessionMap);
        Json.send(resp, 200, Map.of("user", sessionMap, "csrfToken", csrf(session)));
    }

    private void handleOwnerRegister(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException, CampusNestException {
        String name = getPartOrParameter(req, "name");
        String email = getPartOrParameter(req, "email");
        String phone = getPartOrParameter(req, "phone");
        String address = getPartOrParameter(req, "address");
        String password = getPartOrParameterRaw(req, "password");
        Part proofPart = null;
        try {
            proofPart = req.getPart("idProof");
        } catch (Exception ignored) {}

        String originalName = (proofPart != null) ? Objects.toString(proofPart.getSubmittedFileName(), "") : "";
        String contentType = (proofPart != null) ? Objects.toString(proofPart.getContentType(), "") : "";
        InputStream is = (proofPart != null) ? proofPart.getInputStream() : null;
        long size = (proofPart != null) ? proofPart.getSize() : 0;
        String storageDir = System.getenv("CAMPUSNEST_UPLOAD_DIR");

        OwnerUser owner = authService.registerOwner(name, email, phone, address, password,
                originalName, contentType, is, size, storageDir);
        Json.send(resp, 201, Map.of("user", owner.toSessionMap(), "message", "Registration received. An admin must review your details."));
    }

    private void handleCreateInquiry(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        long listingId = intVal(body, "listingId", 0);
        String name = stringVal(body, "name");
        String phone = stringVal(body, "phone");
        String message = stringVal(body, "message");

        Map<String, Object> user = sessionUser(req);
        Long studentUserId = (user != null && user.get("id") instanceof Number n) ? n.longValue() : null;

        Inquiry inquiry = new Inquiry();
        inquiry.setPgId(listingId);
        inquiry.setStudentUserId(studentUserId);
        inquiry.setSenderName(name);
        inquiry.setSenderPhone(phone);
        inquiry.setMessage(message);

        inquiryService.submitInquiry(inquiry);
        Json.send(resp, 201, Map.of("ok", true, "message", "Your inquiry was sent to the owner."));
    }

    private void handleCreateListing(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        long ownerId = getSessionOwnerId(req);

        PgListing listing = parseListingFromBody(body);
        PgListing created = listingService.createListing(ownerId, listing);
        Json.send(resp, 201, Map.of("id", created.getId(), "status", "PENDING", "message", "Listing submitted for admin review."));
    }

    private void handleUpdateListing(HttpServletRequest req, HttpServletResponse resp, long listingId)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        long ownerId = getSessionOwnerId(req);

        PgListing listing = parseListingFromBody(body);
        listing.setId(listingId);
        listingService.updateListing(ownerId, listing);
        Json.send(resp, 200, Map.of("ok", true));
    }

    private void handleUpdateVacancy(HttpServletRequest req, HttpServletResponse resp, long listingId)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        int vacant = intVal(body, "vacant", -1);
        long ownerId = getSessionOwnerId(req);

        listingService.updateVacancy(ownerId, listingId, vacant);
        Json.send(resp, 200, Map.of("ok", true, "vacant", vacant));
    }

    private void handleCreateFeedback(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        String name = stringVal(body, "name");
        String email = stringVal(body, "email");
        String message = stringVal(body, "message");

        Map<String, Object> user = sessionUser(req);
        Long userId = (user != null && user.get("id") instanceof Number n) ? n.longValue() : null;

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setName(name);
        feedback.setEmail(email.isBlank() ? null : email);
        feedback.setMessage(message);

        feedbackService.submitFeedback(feedback);
        Json.send(resp, 201, Map.of("ok", true));
    }

    private void handleUpdateOwnerStatus(HttpServletRequest req, HttpServletResponse resp, long ownerId)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        String statusStr = stringVal(body, "status");
        OwnerStatus status = OwnerStatus.fromString(statusStr);

        adminService.updateOwnerStatus(ownerId, status);
        Json.send(resp, 200, Map.of("ok", true, "status", status.name()));
    }

    private void handleUpdateListingModeration(HttpServletRequest req, HttpServletResponse resp, long listingId)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        String statusStr = stringVal(body, "status");
        ModerationStatus status = ModerationStatus.fromString(statusStr);

        listingService.updateModerationStatus(listingId, status);
        Json.send(resp, 200, Map.of("ok", true, "status", status.name()));
    }

    private void handleToggleListingVisibility(HttpServletRequest req, HttpServletResponse resp, long listingId)
            throws IOException, CampusNestException {
        Map<String, Object> body = Json.read(req);
        Object v = body.get("visible");
        if (!(v instanceof Boolean visible)) {
            throw new ValidationException("visible must be true or false.");
        }
        listingService.toggleVisibility(listingId, visible);
        Json.send(resp, 200, Map.of("ok", true, "visible", visible));
    }

    private void handleOwnerProofDownload(HttpServletRequest req, HttpServletResponse resp, long ownerId)
            throws IOException, CampusNestException {
        String storageDir = System.getenv("CAMPUSNEST_UPLOAD_DIR");
        Path file = adminService.getOwnerProofPath(ownerId, storageDir);
        String mediaType = adminService.getOwnerProofMediaType(ownerId);

        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Content-Disposition", "inline; filename=\"owner-proof\"");
        resp.setContentType(mediaType);
        Files.copy(file, resp.getOutputStream());
    }

    // Helper methods
    private PgListing parseListingFromBody(Map<String, Object> body) throws ValidationException {
        String name = stringVal(body, "name");
        String genderStr = stringVal(body, "gender");
        String roomStr = stringVal(body, "room");
        String address = stringVal(body, "address");
        int rent = intVal(body, "rent", -1);
        int total = intVal(body, "total", -1);
        int vacant = intVal(body, "vacant", -1);
        double distance = doubleVal(body, "distance", -1);
        boolean food = Boolean.TRUE.equals(body.get("food"));
        String desc = stringVal(body, "desc");

        GenderType gender = GenderType.fromString(genderStr);
        RoomType roomType = RoomType.fromString(roomStr);

        if (gender == null || roomType == null) {
            throw new ValidationException("Invalid gender category or room type.");
        }

        List<String> amenities = new ArrayList<>();
        if (body.get("amenities") instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) amenities.add(String.valueOf(item));
            }
        }

        PgListing listing = new PgListing();
        listing.setPgName(name);
        listing.setGenderType(gender);
        listing.setMonthlyRent(BigDecimal.valueOf(rent));
        listing.setRoomType(roomType);
        listing.setFoodFacility(food);
        listing.setTotalBeds(total);
        listing.setVacantBeds(vacant);
        listing.setDistFromCoet(distance);
        listing.setPgAddress(address);
        listing.setAmenities(amenities);
        listing.setDescription(desc);
        return listing;
    }

    private ListingFilterCriteria buildCriteriaFromRequest(HttpServletRequest req, boolean adminView) {
        ListingFilterCriteria criteria = new ListingFilterCriteria();
        criteria.setAdminView(adminView);
        criteria.setQuery(req.getParameter("q"));

        String gender = req.getParameter("gender");
        if (gender != null && !gender.isBlank()) {
            criteria.setGender(GenderType.fromString(gender));
        }

        try {
            String rent = req.getParameter("maxRent");
            if (rent != null) criteria.setMaxRent(Integer.parseInt(rent));
        } catch (Exception ignored) {}

        String room = req.getParameter("room");
        if (room != null && !room.isBlank()) {
            criteria.setRoomType(RoomType.fromString(room));
        }

        if ("true".equalsIgnoreCase(req.getParameter("food"))) {
            criteria.setFoodFacility(true);
        }

        try {
            String dist = req.getParameter("maxDistance");
            if (dist != null) criteria.setMaxDistance(Double.parseDouble(dist));
        } catch (Exception ignored) {}

        criteria.setSortBy(req.getParameter("sort"));
        return criteria;
    }

    private String path(HttpServletRequest req) {
        return Optional.ofNullable(req.getPathInfo()).orElse("/");
    }

    private String csrf(HttpSession session) {
        String token = (String) session.getAttribute("csrf");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("csrf", token);
        }
        return token;
    }

    private boolean isCsrfValid(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        String expected = (session == null) ? null : (String) session.getAttribute("csrf");
        String actual = req.getHeader("X-CSRF-Token");
        if (expected == null || actual == null || !MessageDigest.isEqual(expected.getBytes(), actual.getBytes())) {
            Json.send(resp, 403, Json.error("Session token expired. Refresh the page and try again."));
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sessionUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object u = (s == null) ? null : s.getAttribute("user");
        return (u instanceof Map<?, ?>) ? (Map<String, Object>) u : null;
    }

    private boolean requireRole(HttpServletRequest req, HttpServletResponse resp, String requiredRole) throws IOException {
        Map<String, Object> user = sessionUser(req);
        if (user == null) {
            Json.send(resp, 401, Json.error("Sign in to continue."));
            return false;
        }
        if (!requiredRole.equals(user.get("role"))) {
            Json.send(resp, 403, Json.error("This account cannot access that feature."));
            return false;
        }
        if ("OWNER".equals(requiredRole) && !"APPROVED".equals(user.get("status"))) {
            Json.send(resp, 403, Json.error("Owner verification is still pending approval."));
            return false;
        }
        return true;
    }

    private long getSessionOwnerId(HttpServletRequest req) {
        Map<String, Object> user = sessionUser(req);
        if (user != null && user.get("ownerId") instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private static String stringVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val == null) ? "" : String.valueOf(val).trim();
    }

    private static int intVal(Map<String, Object> map, String key, int fallback) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double doubleVal(Map<String, Object> map, String key, double fallback) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String trim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String getPartOrParameter(HttpServletRequest req, String name) {
        String val = req.getParameter(name);
        if (val != null && !val.isBlank()) return val.trim();
        try {
            Part part = req.getPart(name);
            if (part != null) {
                try (InputStream is = part.getInputStream()) {
                    return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String getPartOrParameterRaw(HttpServletRequest req, String name) {
        String val = req.getParameter(name);
        if (val != null && !val.isEmpty()) return val;
        try {
            Part part = req.getPart(name);
            if (part != null) {
                try (InputStream is = part.getInputStream()) {
                    return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
