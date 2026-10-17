package edu.coet.campusnest;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@WebServlet("/api/*")
@MultipartConfig(maxFileSize=5_242_880, maxRequestSize=6_291_456, fileSizeThreshold=1_048_576)
public class ApiServlet extends HttpServlet {
    private static final String PUBLIC_COLUMNS = "p.pg_id AS id,p.pg_name AS name,o.owner_name AS owner,p.gender_type AS gender,p.monthly_rent AS rent,p.room_type AS room,p.food_facility AS food,p.total_beds AS total,p.vacant_beds AS vacant,p.dist_from_coet AS distance,p.pg_address AS address,p.amenities_json AS amenities,p.description AS description,o.contact_no AS phone,p.is_demo AS is_demo";

    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        try { super.service(req, resp); }
        catch (Exception ex) {
            getServletContext().log("CampusNest API request failed", ex);
            if (!resp.isCommitted()) Json.send(resp, 500, Json.error("The server could not complete this request."));
        }
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = path(req);
        try {
            if (path.equals("/health")) { boolean ready=databaseAlive();Json.send(resp,ready?200:503,Map.of("status",ready?"ok":"unavailable","database",ready));return; }
            if (path.equals("/auth/csrf")) { Json.send(resp, 200, Map.of("csrfToken", csrf(req.getSession(true)))); return; }
            if (path.equals("/auth/me")) { Map<String,Object> me=new LinkedHashMap<>();me.put("user",sessionUser(req));me.put("csrfToken",csrf(req.getSession(true)));Json.send(resp,200,me);return; }
            if (path.equals("/listings")) { Json.send(resp, 200, listListings(req, false)); return; }
            if (path.matches("/listings/\\d+")) { Map<String,Object> item=oneListing(Long.parseLong(path.substring(10)),false); if(item==null){Json.send(resp,404,Json.error("PG not found"));return;} Json.send(resp,200,item); return; }
            if (path.equals("/owner/listings")) { if(!require(req,resp,"OWNER"))return; Json.send(resp,200,ownerListings(req)); return; }
            if (path.equals("/owner/inquiries")) { if(!require(req,resp,"OWNER"))return; Json.send(resp,200,ownerInquiries(req)); return; }
            if (path.equals("/admin/dashboard")) { if(!require(req,resp,"ADMIN"))return; Json.send(resp,200,adminCounts()); return; }
            if (path.equals("/admin/owners")) { if(!require(req,resp,"ADMIN"))return; Json.send(resp,200,ownerQueue()); return; }
            if (path.matches("/admin/owners/\\d+/proof")) { if(!require(req,resp,"ADMIN"))return; ownerProof(req,resp,Long.parseLong(path.split("/")[3])); return; }
            if (path.equals("/admin/listings")) { if(!require(req,resp,"ADMIN"))return; Json.send(resp,200,listListings(req,true)); return; }
            if (path.equals("/admin/feedback")) { if(!require(req,resp,"ADMIN"))return; Json.send(resp,200,feedback()); return; }
            Json.send(resp,404,Json.error("Endpoint not found"));
        } catch (SQLException e) { throw new IOException(e); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path=path(req);
        if(!csrfValid(req,resp))return;
        try {
            if(path.equals("/auth/register")){studentRegister(req,resp);return;}
            if(path.equals("/auth/login")){login(req,resp);return;}
            if(path.equals("/auth/logout")){req.getSession(false).invalidate();Json.send(resp,200,Map.of("ok",true));return;}
            if(path.equals("/owners/register")){ownerRegister(req,resp);return;}
            if(path.equals("/inquiries")){createInquiry(req,resp);return;}
            if(path.equals("/owner/listings")){if(!require(req,resp,"OWNER"))return;createListing(req,resp);return;}
            if(path.equals("/feedback")){createFeedback(req,resp);return;}
            Json.send(resp,404,Json.error("Endpoint not found"));
        } catch(SQLException e){throw new IOException(e);}
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path=path(req); if(!csrfValid(req,resp))return;
        try {
            if(path.matches("/owner/listings/\\d+")){if(!require(req,resp,"OWNER"))return;updateListing(req,resp,Long.parseLong(path.split("/")[3]));return;}
            if(path.matches("/owner/listings/\\d+/vacancy")){if(!require(req,resp,"OWNER"))return;updateVacancy(req,resp,Long.parseLong(path.split("/")[3]));return;}
            if(path.matches("/admin/owners/\\d+/status")){if(!require(req,resp,"ADMIN"))return;updateOwnerStatus(req,resp,Long.parseLong(path.split("/")[3]));return;}
            if(path.matches("/admin/listings/\\d+/status")){if(!require(req,resp,"ADMIN"))return;updateListingStatus(req,resp,Long.parseLong(path.split("/")[3]));return;}
            if(path.matches("/admin/listings/\\d+")){if(!require(req,resp,"ADMIN"))return;toggleListing(req,resp,Long.parseLong(path.split("/")[3]));return;}
            Json.send(resp,404,Json.error("Endpoint not found"));
        }catch(SQLException e){throw new IOException(e);}
    }

    @Override protected void doDelete(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        String path=path(req);if(!csrfValid(req,resp))return;if(!require(req,resp,"ADMIN"))return;
        if(!path.matches("/admin/listings/\\d+")){Json.send(resp,404,Json.error("Endpoint not found"));return;}
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("DELETE FROM pg_details WHERE pg_id=?")){ps.setLong(1,Long.parseLong(path.split("/")[3]));if(ps.executeUpdate()==0)Json.send(resp,404,Json.error("Listing not found"));else Json.send(resp,200,Map.of("ok",true));}
        catch(SQLException e){throw new IOException(e);}
    }

    private String path(HttpServletRequest req){return Optional.ofNullable(req.getPathInfo()).orElse("/");}
    private boolean databaseAlive(){try(Connection c=Db.connect()){return c.isValid(2);}catch(SQLException e){return false;}}
    private String csrf(HttpSession session){String token=(String)session.getAttribute("csrf");if(token==null){token=UUID.randomUUID().toString();session.setAttribute("csrf",token);}return token;}
    private boolean csrfValid(HttpServletRequest req,HttpServletResponse resp)throws IOException{HttpSession s=req.getSession(false);String expected=s==null?null:(String)s.getAttribute("csrf");String actual=req.getHeader("X-CSRF-Token");if(expected==null||actual==null||!java.security.MessageDigest.isEqual(expected.getBytes(),actual.getBytes())){Json.send(resp,403,Json.error("Session token expired. Refresh the page and try again."));return false;}return true;}
    @SuppressWarnings("unchecked") private Map<String,Object> sessionUser(HttpServletRequest req){Object u=req.getSession(false)==null?null:req.getSession(false).getAttribute("user");return u instanceof Map<?,?>?(Map<String,Object>)u:null;}
    private boolean require(HttpServletRequest req,HttpServletResponse resp,String role)throws IOException{Map<String,Object> user=sessionUser(req);if(user==null){Json.send(resp,401,Json.error("Sign in to continue."));return false;}if(!role.equals(user.get("role"))){Json.send(resp,403,Json.error("This account cannot access that page."));return false;}if("OWNER".equals(role)&&!"APPROVED".equals(user.get("status"))){Json.send(resp,403,Json.error("Owner verification is still pending."));return false;}return true;}
    private static String str(Map<String,Object> m,String k){Object v=m.get(k);return v==null?"":String.valueOf(v).trim();}
    private static int num(Map<String,Object> m,String k,int fallback){Object v=m.get(k);if(v instanceof Number n)return n.intValue();try{return Integer.parseInt(String.valueOf(v));}catch(Exception e){return fallback;}}
    private static double decimal(Map<String,Object> m,String k,double fallback){Object v=m.get(k);if(v instanceof Number n)return n.doubleValue();try{return Double.parseDouble(String.valueOf(v));}catch(Exception e){return fallback;}}
    private static String email(String value){return value.trim().toLowerCase(Locale.ROOT);}
    private static String now(ResultSet rs,String column)throws SQLException{Timestamp t=rs.getTimestamp(column);return t==null?null:t.toInstant().toString();}
    private static List<String> amenityList(String json){try{return Json.MAPPER.readValue(json,new TypeReference<>(){});}catch(Exception e){return List.of();}}

    private List<Map<String,Object>> listListings(HttpServletRequest req,boolean admin)throws SQLException{
        StringBuilder sql=new StringBuilder("SELECT ").append(PUBLIC_COLUMNS).append(",p.moderation_status,p.is_visible FROM pg_details p JOIN pg_owners o ON o.owner_id=p.owner_id LEFT JOIN users u ON o.user_id=u.user_id WHERE ");
        if(admin)sql.append("1=1 ");else sql.append("p.is_visible=TRUE AND p.moderation_status='APPROVED' AND o.owner_status='APPROVED' AND (o.user_id IS NULL OR u.account_status='ACTIVE') ");
        List<Object> args=new ArrayList<>();
        String q=req.getParameter("q");if(q!=null&&!q.isBlank()){sql.append("AND (p.pg_name LIKE ? OR p.pg_address LIKE ?) ");args.add("%"+q.trim()+"%");args.add("%"+q.trim()+"%");}
        String gender=req.getParameter("gender");if("Boys".equals(gender)||"Girls".equals(gender)){sql.append("AND p.gender_type=? ");args.add(gender.toUpperCase(Locale.ROOT));}
        addIntFilter(sql,args,req,"maxRent","AND p.monthly_rent<=?");String room=req.getParameter("room");if("Single".equals(room)||"Shared".equals(room)){sql.append("AND p.room_type=? ");args.add(room);}
        if("true".equalsIgnoreCase(req.getParameter("food")))sql.append("AND p.food_facility=TRUE ");addDoubleFilter(sql,args,req,"maxDistance","AND p.dist_from_coet<=?");
        String sort=req.getParameter("sort");sql.append(switch(Objects.toString(sort,"")){case "rent-asc"->"ORDER BY p.monthly_rent ASC,p.dist_from_coet ASC";case "distance"->"ORDER BY p.dist_from_coet ASC,p.monthly_rent ASC";default->"ORDER BY p.is_demo DESC,p.created_at DESC";});
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement(sql.toString())){for(int i=0;i<args.size();i++)ps.setObject(i+1,args.get(i));try(ResultSet rs=ps.executeQuery()){List<Map<String,Object>> out=new ArrayList<>();while(rs.next())out.add(listingMap(rs,admin));return out;}}
    }
    private static void addIntFilter(StringBuilder sql,List<Object> args,HttpServletRequest req,String key,String clause){try{int v=Integer.parseInt(req.getParameter(key));if(v>0){sql.append(clause).append(' ');args.add(v);}}catch(Exception ignored){}}
    private static void addDoubleFilter(StringBuilder sql,List<Object> args,HttpServletRequest req,String key,String clause){try{double v=Double.parseDouble(req.getParameter(key));if(v>=0){sql.append(clause).append(' ');args.add(v);}}catch(Exception ignored){}}
    private Map<String,Object> oneListing(long id,boolean admin)throws SQLException{
        String sql="SELECT "+PUBLIC_COLUMNS+",p.moderation_status,p.is_visible FROM pg_details p JOIN pg_owners o ON o.owner_id=p.owner_id LEFT JOIN users u ON o.user_id=u.user_id WHERE p.pg_id=?"+(admin?"":" AND p.is_visible=TRUE AND p.moderation_status='APPROVED' AND o.owner_status='APPROVED' AND (o.user_id IS NULL OR u.account_status='ACTIVE')");
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?listingMap(rs,admin):null;}}
    }
    private Map<String,Object> listingMap(ResultSet rs,boolean admin)throws SQLException{
        Map<String,Object> m=new LinkedHashMap<>();long id=rs.getLong("id");m.put("id",id);m.put("name",rs.getString("name"));m.put("owner",rs.getString("owner"));m.put("gender",rs.getString("gender").equals("BOYS")?"Boys":"Girls");m.put("rent",rs.getBigDecimal("rent").intValue());m.put("room",rs.getString("room"));m.put("food",rs.getBoolean("food"));m.put("total",rs.getInt("total"));m.put("vacant",rs.getInt("vacant"));m.put("distance",rs.getBigDecimal("distance").doubleValue());m.put("address",rs.getString("address"));m.put("amenities",amenityList(rs.getString("amenities")));m.put("desc",rs.getString("description"));m.put("phone",rs.getString("phone"));m.put("scene", "scene-"+(char)('a'+(id%3)));m.put("demo",rs.getBoolean("is_demo"));if(admin){m.put("status",rs.getString("moderation_status"));m.put("visible",rs.getBoolean("is_visible"));}return m;
    }

    private void studentRegister(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object> b=Json.read(req);String name=str(b,"name"),email=email(str(b,"email")),password=str(b,"password"),phone=str(b,"phone");
        if(name.isBlank()||email.isBlank()||password.length()<8){Json.send(resp,400,Json.error("Enter your name, a valid email and a password of at least 8 characters."));return;}
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO users(name,email,phone_no,password_hash,role,account_status) VALUES(?,?,?,?,'STUDENT','ACTIVE')",Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1,name);ps.setString(2,email);ps.setString(3,phone.isBlank()?null:phone);ps.setString(4,Passwords.hash(password));ps.executeUpdate();long id;try(ResultSet keys=ps.getGeneratedKeys()){keys.next();id=keys.getLong(1);}Map<String,Object> user=Map.of("id",id,"name",name,"email",email,"role","STUDENT","status","ACTIVE");req.changeSessionId();req.getSession(true).setAttribute("user",user);Json.send(resp,201,Map.of("user",user,"csrfToken",csrf(req.getSession(false))));
        }catch(SQLIntegrityConstraintViolationException e){Json.send(resp,409,Json.error("An account already uses that email."));}
    }

    private void login(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object> b=Json.read(req);String email=email(str(b,"email")),password=str(b,"password");
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("SELECT u.user_id,u.name,u.email,u.password_hash,u.role,u.account_status,o.owner_id,o.owner_status FROM users u LEFT JOIN pg_owners o ON o.user_id=u.user_id WHERE u.email=?")){
            ps.setString(1,email);try(ResultSet rs=ps.executeQuery()){if(!rs.next()||!Passwords.verify(password,rs.getString("password_hash"))){Json.send(resp,401,Json.error("Email or password is incorrect."));return;}if("DISABLED".equals(rs.getString("account_status"))){Json.send(resp,403,Json.error("This account is disabled."));return;}Map<String,Object> user=new LinkedHashMap<>();user.put("id",rs.getLong("user_id"));user.put("name",rs.getString("name"));user.put("email",rs.getString("email"));user.put("role",rs.getString("role"));user.put("status","OWNER".equals(rs.getString("role"))?rs.getString("owner_status"):rs.getString("account_status"));if("OWNER".equals(rs.getString("role")))user.put("ownerId",rs.getLong("owner_id"));req.changeSessionId();HttpSession session=req.getSession(true);session.setAttribute("user",user);Json.send(resp,200,Map.of("user",user,"csrfToken",csrf(session)));}
        }
    }

    private void ownerRegister(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException,ServletException{
        String name=trim(req.getParameter("name")),mail=email(trim(req.getParameter("email"))),phone=trim(req.getParameter("phone")),address=trim(req.getParameter("address")),password=req.getParameter("password");Part proof=req.getPart("idProof");String original=proof==null?"":Objects.toString(proof.getSubmittedFileName(),"");
        String ext=original.toLowerCase(Locale.ROOT).endsWith(".pdf")?".pdf":original.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg)$")?".jpg":original.toLowerCase(Locale.ROOT).endsWith(".png")?".png":"";
        String type=proof==null?"":Objects.toString(proof.getContentType(),"");
        if(name.isBlank()||mail.isBlank()||password==null||password.length()<8||phone.isBlank()||address.isBlank()||proof==null||proof.getSize()<1||ext.isBlank()||!(type.equals("application/pdf")||type.equals("image/jpeg")||type.equals("image/png"))){Json.send(resp,400,Json.error("Enter your name, email, phone, address, password and a PDF, JPG or PNG ID proof."));return;}
        String storage=System.getenv().getOrDefault("CAMPUSNEST_UPLOAD_DIR",Path.of(System.getProperty("user.dir"),"campusnest-private","owner-proofs").toString());Path directory=Path.of(storage).toAbsolutePath().normalize();Files.createDirectories(directory);String stored=UUID.randomUUID()+ext;Path target=directory.resolve(stored).normalize();if(!target.getParent().equals(directory)){Json.send(resp,400,Json.error("Invalid file."));return;}
        try(Connection c=Db.connect()){
            c.setAutoCommit(false);
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO users(name,email,phone_no,password_hash,role,account_status) VALUES(?,?,?,?,'OWNER','ACTIVE')",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,name);ps.setString(2,mail);ps.setString(3,phone);ps.setString(4,Passwords.hash(password));ps.executeUpdate();long id;try(ResultSet keys=ps.getGeneratedKeys()){keys.next();id=keys.getLong(1);}proof.write(target.toString());
                try(PreparedStatement owner=c.prepareStatement("INSERT INTO pg_owners(user_id,owner_name,contact_no,address,id_proof,proof_original_name,proof_media_type,owner_status) VALUES(?,?,?,?,?,?,?,'PENDING')",Statement.RETURN_GENERATED_KEYS)){owner.setLong(1,id);owner.setString(2,name);owner.setString(3,phone);owner.setString(4,address);owner.setString(5,stored);owner.setString(6,original.substring(Math.max(0,original.length()-180)));owner.setString(7,type);owner.executeUpdate();}c.commit();Map<String,Object> user=Map.of("id",id,"name",name,"email",mail,"role","OWNER","status","PENDING");Json.send(resp,201,Map.of("user",user,"message","Registration received. An admin must review your details."));
            }catch(Exception e){c.rollback();Files.deleteIfExists(target);if(e instanceof SQLIntegrityConstraintViolationException){Json.send(resp,409,Json.error("An account already uses that email."));return;}if(e instanceof IOException io)throw io;if(e instanceof SQLException sql)throw sql;throw new IOException(e);}finally{c.setAutoCommit(true);}
        }
    }

    private void createInquiry(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);long listing=num(b,"listingId",0);String name=str(b,"name"),phone=str(b,"phone"),message=str(b,"message");if(listing<1||name.isBlank()||phone.isBlank()||message.isBlank()||message.length()>2000){Json.send(resp,400,Json.error("Complete your name, phone number and message."));return;}
        Map<String,Object> user=sessionUser(req);try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO inquiries(pg_id,student_user_id,sender_name,sender_phone,message) SELECT p.pg_id,?,?,?,? FROM pg_details p JOIN pg_owners o ON o.owner_id=p.owner_id WHERE p.pg_id=? AND p.is_visible=TRUE AND p.moderation_status='APPROVED' AND o.owner_status='APPROVED'",Statement.RETURN_GENERATED_KEYS)){
            ps.setObject(1,user==null?null:user.get("id"));ps.setString(2,name);ps.setString(3,phone);ps.setString(4,message);ps.setLong(5,listing);if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("This PG listing is no longer available."));return;}Json.send(resp,201,Map.of("ok",true,"message","Your inquiry was sent to the owner."));
        }
    }

    private void createFeedback(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String name=str(b,"name"),mail=email(str(b,"email")),message=str(b,"message");if(name.isBlank()||message.isBlank()||message.length()>2000){Json.send(resp,400,Json.error("Enter your name and feedback message."));return;}Map<String,Object> u=sessionUser(req);try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO feedback(user_id,name,email,message) VALUES(?,?,?,?)")){ps.setObject(1,u==null?null:u.get("id"));ps.setString(2,name);ps.setString(3,mail.isBlank()?null:mail);ps.setString(4,message);ps.executeUpdate();Json.send(resp,201,Map.of("ok",true));}
    }

    private static String trim(String s){return s==null?"":s.trim();}

    private List<Map<String,Object>> ownerListings(HttpServletRequest req)throws SQLException{
        long owner=((Number)sessionUser(req).get("ownerId")).longValue();try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("SELECT "+PUBLIC_COLUMNS+",p.moderation_status,p.is_visible FROM pg_details p JOIN pg_owners o ON o.owner_id=p.owner_id WHERE p.owner_id=? ORDER BY p.updated_at DESC")){ps.setLong(1,owner);try(ResultSet rs=ps.executeQuery()){List<Map<String,Object>> a=new ArrayList<>();while(rs.next())a.add(listingMap(rs,true));return a;}}
    }
    private void createListing(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String name=str(b,"name"),gender=str(b,"gender"),room=str(b,"room"),address=str(b,"address");int rent=num(b,"rent",-1),total=num(b,"total",-1),vacant=num(b,"vacant",-1);double distance=decimal(b,"distance",-1);boolean food=Boolean.TRUE.equals(b.get("food"));List<?> amenityRaw=b.get("amenities") instanceof List<?> l?l:List.of();String amenities=Json.MAPPER.writeValueAsString(amenityRaw);
        if(name.isBlank()||!(gender.equals("Boys")||gender.equals("Girls"))||!(room.equals("Shared")||room.equals("Single"))||rent<1||total<1||vacant<0||vacant>total||distance<0||distance>9.9||address.isBlank()){Json.send(resp,400,Json.error("Check the PG details and bed counts."));return;}
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO pg_details(owner_id,pg_name,gender_type,monthly_rent,room_type,food_facility,total_beds,vacant_beds,dist_from_coet,pg_address,amenities_json,description,moderation_status,is_visible,is_demo) VALUES(?,?,?,?,?,?,?,?,?,?,?,?, 'PENDING',TRUE,FALSE)",Statement.RETURN_GENERATED_KEYS)){
            int i=1;ps.setObject(i++,sessionUser(req).get("ownerId"));ps.setString(i++,name);ps.setString(i++,gender.toUpperCase(Locale.ROOT));ps.setInt(i++,rent);ps.setString(i++,room);ps.setBoolean(i++,food);ps.setInt(i++,total);ps.setInt(i++,vacant);ps.setDouble(i++,distance);ps.setString(i++,address);ps.setString(i++,amenities);ps.setString(i,str(b,"desc"));ps.executeUpdate();try(ResultSet keys=ps.getGeneratedKeys()){keys.next();Json.send(resp,201,Map.of("id",keys.getLong(1),"status","PENDING","message","Listing submitted for admin review."));}
        }
    }
    private void updateListing(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String name=str(b,"name"),gender=str(b,"gender"),room=str(b,"room"),address=str(b,"address");int rent=num(b,"rent",-1),total=num(b,"total",-1),vacant=num(b,"vacant",-1);double distance=decimal(b,"distance",-1);if(name.isBlank()||!(gender.equals("Boys")||gender.equals("Girls"))||!(room.equals("Shared")||room.equals("Single"))||rent<1||total<1||vacant<0||vacant>total||distance<0||distance>9.9||address.isBlank()){Json.send(resp,400,Json.error("Check the PG details and bed counts."));return;}String amenities=Json.MAPPER.writeValueAsString(b.get("amenities") instanceof List<?> l?l:List.of());
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_details SET pg_name=?,gender_type=?,monthly_rent=?,room_type=?,food_facility=?,total_beds=?,vacant_beds=?,dist_from_coet=?,pg_address=?,amenities_json=?,description=? WHERE pg_id=? AND owner_id=?")){int i=1;ps.setString(i++,name);ps.setString(i++,gender.toUpperCase(Locale.ROOT));ps.setInt(i++,rent);ps.setString(i++,room);ps.setBoolean(i++,Boolean.TRUE.equals(b.get("food")));ps.setInt(i++,total);ps.setInt(i++,vacant);ps.setDouble(i++,distance);ps.setString(i++,address);ps.setString(i++,amenities);ps.setString(i++,str(b,"desc"));ps.setLong(i++,id);ps.setObject(i,sessionUser(req).get("ownerId"));if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("PG listing not found."));return;}Json.send(resp,200,Map.of("ok",true));}
    }
    private void updateVacancy(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);int value=num(b,"vacant",-1);if(value<0){Json.send(resp,400,Json.error("Vacant beds must be zero or more."));return;}try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_details SET vacant_beds=? WHERE pg_id=? AND owner_id=? AND ?<=total_beds")){ps.setInt(1,value);ps.setLong(2,id);ps.setObject(3,sessionUser(req).get("ownerId"));ps.setInt(4,value);if(ps.executeUpdate()==0){Json.send(resp,400,Json.error("Could not update vacancy. Check the listing and total beds."));return;}Json.send(resp,200,Map.of("ok",true,"vacant",value));}
    }
    private List<Map<String,Object>> ownerInquiries(HttpServletRequest req)throws SQLException{
        String sql="SELECT i.id,i.sender_name,i.sender_phone,i.message,i.status,i.created_at,p.pg_name listing_name FROM inquiries i JOIN pg_details p ON p.pg_id=i.pg_id WHERE p.owner_id=? ORDER BY i.created_at DESC";try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement(sql)){ps.setObject(1,sessionUser(req).get("ownerId"));try(ResultSet rs=ps.executeQuery()){List<Map<String,Object>> a=new ArrayList<>();while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("sender_name"));m.put("phone",rs.getString("sender_phone"));m.put("message",rs.getString("message"));m.put("status",rs.getString("status"));m.put("date",now(rs,"created_at"));m.put("listing",rs.getString("listing_name"));a.add(m);}return a;}}
    }
    private Map<String,Object> adminCounts()throws SQLException{
        try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT (SELECT COUNT(*) FROM pg_owners WHERE owner_status='PENDING') pending_owners,(SELECT COUNT(*) FROM pg_details WHERE is_visible=TRUE AND moderation_status='APPROVED') visible_listings,(SELECT COUNT(*) FROM inquiries WHERE status='NEW') inquiries,(SELECT COUNT(*) FROM feedback WHERE status='NEW') feedback")){rs.next();return Map.of("pendingOwners",rs.getInt(1),"visibleListings",rs.getInt(2),"inquiries",rs.getInt(3),"feedback",rs.getInt(4));}
    }
    private List<Map<String,Object>> ownerQueue()throws SQLException{
        String sql="SELECT o.owner_id AS id,u.name,u.email,o.contact_no AS phone,o.owner_status AS status,o.created_at,o.proof_original_name AS original_name FROM pg_owners o JOIN users u ON u.user_id=o.user_id ORDER BY FIELD(o.owner_status,'PENDING','APPROVED','REJECTED'),o.created_at DESC";try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet rs=s.executeQuery(sql)){List<Map<String,Object>>a=new ArrayList<>();while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("name"));m.put("email",rs.getString("email"));m.put("phone",rs.getString("phone"));m.put("status",rs.getString("status"));m.put("date",now(rs,"created_at"));m.put("proof",rs.getString("original_name"));a.add(m);}return a;}
    }
    private void ownerProof(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        String stored=null,type=null;
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("SELECT id_proof,proof_media_type FROM pg_owners WHERE owner_id=?")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(rs.next()){stored=rs.getString(1);type=rs.getString(2);}}}
        if(stored==null){Json.send(resp,404,Json.error("Owner proof not found."));return;}
        String base=System.getenv().getOrDefault("CAMPUSNEST_UPLOAD_DIR",Path.of(System.getProperty("user.dir"),"campusnest-private","owner-proofs").toString());Path dir=Path.of(base).toAbsolutePath().normalize(),file=dir.resolve(stored).normalize();
        if(!file.getParent().equals(dir)||!Files.isRegularFile(file)){Json.send(resp,404,Json.error("Owner proof not found."));return;}
        String safeType=Set.of("application/pdf","image/jpeg","image/png").contains(type)?type:"application/octet-stream";
        resp.setHeader("Cache-Control","no-store");resp.setHeader("Content-Disposition","inline; filename=\"owner-proof\"");resp.setContentType(safeType);Files.copy(file,resp.getOutputStream());
    }
    private void updateOwnerStatus(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String status=str(b,"status");if(!Set.of("APPROVED","REJECTED").contains(status)){Json.send(resp,400,Json.error("Choose APPROVED or REJECTED."));return;}try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_owners SET owner_status=?,proof_reviewed_at=CURRENT_TIMESTAMP WHERE owner_id=?")){ps.setString(1,status);ps.setLong(2,id);if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("Owner not found."));return;}Json.send(resp,200,Map.of("ok",true,"status",status));}
    }
    private void toggleListing(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);Object v=b.get("visible");if(!(v instanceof Boolean visible)){Json.send(resp,400,Json.error("visible must be true or false."));return;}try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_details SET is_visible=? WHERE pg_id=?")){ps.setBoolean(1,visible);ps.setLong(2,id);if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("Listing not found."));return;}Json.send(resp,200,Map.of("ok",true,"visible",visible));}
    }
    private void updateListingStatus(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String status=str(b,"status");if(!Set.of("APPROVED","REJECTED").contains(status)){Json.send(resp,400,Json.error("Choose APPROVED or REJECTED."));return;}
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_details SET moderation_status=?,is_visible=? WHERE pg_id=?")){ps.setString(1,status);ps.setBoolean(2,status.equals("APPROVED"));ps.setLong(3,id);if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("Listing not found."));return;}Json.send(resp,200,Map.of("ok",true,"status",status));}
    }
    private List<Map<String,Object>> feedback()throws SQLException{
        String sql="SELECT id,name,email,message,status,created_at FROM feedback ORDER BY created_at DESC";try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet rs=s.executeQuery(sql)){List<Map<String,Object>>a=new ArrayList<>();while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("name"));m.put("email",rs.getString("email"));m.put("message",rs.getString("message"));m.put("status",rs.getString("status"));m.put("date",now(rs,"created_at"));a.add(m);}return a;}
    }
}
