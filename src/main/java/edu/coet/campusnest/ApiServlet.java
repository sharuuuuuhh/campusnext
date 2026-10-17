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
    private static final String PUBLIC_COLUMNS = "p.id,p.name,p.owner_name,p.gender,p.monthly_rent,p.room_type,p.food_available,p.beds_total,p.beds_vacant,p.distance_km,p.address,p.amenities_json,p.description,p.contact_phone,p.is_demo";

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
            if (path.equals("/health")) { Json.send(resp, 200, Map.of("status", "ok", "database", databaseAlive())); return; }
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
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("DELETE FROM pg_listings WHERE id=?")){ps.setLong(1,Long.parseLong(path.split("/")[3]));if(ps.executeUpdate()==0)Json.send(resp,404,Json.error("Listing not found"));else Json.send(resp,200,Map.of("ok",true));}
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
        StringBuilder sql=new StringBuilder("SELECT ").append(PUBLIC_COLUMNS).append(",p.moderation_status,p.is_visible FROM pg_listings p LEFT JOIN users u ON p.owner_user_id=u.id WHERE ");
        if(admin)sql.append("1=1 ");else sql.append("p.is_visible=TRUE AND p.moderation_status='APPROVED' AND (p.owner_user_id IS NULL OR u.status='APPROVED') ");
        List<Object> args=new ArrayList<>();
        String q=req.getParameter("q");if(q!=null&&!q.isBlank()){sql.append("AND (p.name LIKE ? OR p.address LIKE ?) ");args.add("%"+q.trim()+"%");args.add("%"+q.trim()+"%");}
        String gender=req.getParameter("gender");if("Boys".equals(gender)||"Girls".equals(gender)){sql.append("AND p.gender=? ");args.add(gender);}
        addIntFilter(sql,args,req,"maxRent","AND p.monthly_rent<=?");String room=req.getParameter("room");if("Single".equals(room)||"Shared".equals(room)){sql.append("AND p.room_type=? ");args.add(room);}
        if("true".equalsIgnoreCase(req.getParameter("food")))sql.append("AND p.food_available=TRUE ");addDoubleFilter(sql,args,req,"maxDistance","AND p.distance_km<=?");
        String sort=req.getParameter("sort");sql.append(switch(Objects.toString(sort,"")){case "rent-asc"->"ORDER BY p.monthly_rent ASC,p.distance_km ASC";case "distance"->"ORDER BY p.distance_km ASC,p.monthly_rent ASC";default->"ORDER BY p.is_demo DESC,p.created_at DESC";});
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement(sql.toString())){for(int i=0;i<args.size();i++)ps.setObject(i+1,args.get(i));try(ResultSet rs=ps.executeQuery()){List<Map<String,Object>> out=new ArrayList<>();while(rs.next())out.add(listingMap(rs,admin));return out;}}
    }
    private static void addIntFilter(StringBuilder sql,List<Object> args,HttpServletRequest req,String key,String clause){try{int v=Integer.parseInt(req.getParameter(key));if(v>0){sql.append(clause).append(' ');args.add(v);}}catch(Exception ignored){}}
    private static void addDoubleFilter(StringBuilder sql,List<Object> args,HttpServletRequest req,String key,String clause){try{double v=Double.parseDouble(req.getParameter(key));if(v>=0){sql.append(clause).append(' ');args.add(v);}}catch(Exception ignored){}}
    private Map<String,Object> oneListing(long id,boolean admin)throws SQLException{
        String sql="SELECT "+PUBLIC_COLUMNS+",p.moderation_status,p.is_visible FROM pg_listings p LEFT JOIN users u ON p.owner_user_id=u.id WHERE p.id=?"+(admin?"":" AND p.is_visible=TRUE AND p.moderation_status='APPROVED' AND (p.owner_user_id IS NULL OR u.status='APPROVED')");
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?listingMap(rs,admin):null;}}
    }
    private Map<String,Object> listingMap(ResultSet rs,boolean admin)throws SQLException{
        Map<String,Object> m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("name"));m.put("owner",rs.getString("owner_name"));m.put("gender",rs.getString("gender"));m.put("rent",rs.getInt("monthly_rent"));m.put("room",rs.getString("room_type"));m.put("food",rs.getBoolean("food_available"));m.put("total",rs.getInt("beds_total"));m.put("vacant",rs.getInt("beds_vacant"));m.put("distance",rs.getBigDecimal("distance_km").doubleValue());m.put("address",rs.getString("address"));m.put("amenities",amenityList(rs.getString("amenities_json")));m.put("desc",rs.getString("description"));m.put("phone",rs.getString("contact_phone"));m.put("scene", "scene-"+(char)('a'+(rs.getLong("id")%3)));m.put("demo",rs.getBoolean("is_demo"));if(admin){m.put("status",rs.getString("moderation_status"));m.put("visible",rs.getBoolean("is_visible"));}return m;
    }

    private void studentRegister(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object> b=Json.read(req);String name=str(b,"name"),email=email(str(b,"email")),password=str(b,"password"),phone=str(b,"phone");
        if(name.isBlank()||email.isBlank()||password.length()<8){Json.send(resp,400,Json.error("Enter your name, a valid email and a password of at least 8 characters."));return;}
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO users(full_name,email,phone,password_hash,role,status) VALUES(?,?,?,?,'STUDENT','ACTIVE')",Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1,name);ps.setString(2,email);ps.setString(3,phone.isBlank()?null:phone);ps.setString(4,Passwords.hash(password));ps.executeUpdate();long id;try(ResultSet keys=ps.getGeneratedKeys()){keys.next();id=keys.getLong(1);}Map<String,Object> user=Map.of("id",id,"name",name,"email",email,"role","STUDENT","status","ACTIVE");req.changeSessionId();req.getSession(true).setAttribute("user",user);Json.send(resp,201,Map.of("user",user,"csrfToken",csrf(req.getSession(false))));
        }catch(SQLIntegrityConstraintViolationException e){Json.send(resp,409,Json.error("An account already uses that email."));}
    }

    private void login(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object> b=Json.read(req);String email=email(str(b,"email")),password=str(b,"password");
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("SELECT id,full_name,email,password_hash,role,status FROM users WHERE email=?")){
            ps.setString(1,email);try(ResultSet rs=ps.executeQuery()){if(!rs.next()||!Passwords.verify(password,rs.getString("password_hash"))){Json.send(resp,401,Json.error("Email or password is incorrect."));return;}Map<String,Object> user=new LinkedHashMap<>();user.put("id",rs.getLong("id"));user.put("name",rs.getString("full_name"));user.put("email",rs.getString("email"));user.put("role",rs.getString("role"));user.put("status",rs.getString("status"));req.changeSessionId();HttpSession session=req.getSession(true);session.setAttribute("user",user);Json.send(resp,200,Map.of("user",user,"csrfToken",csrf(session)));}
        }
    }

    private void ownerRegister(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException,ServletException{
        String name=trim(req.getParameter("name")),mail=email(trim(req.getParameter("email"))),phone=trim(req.getParameter("phone")),password=req.getParameter("password");Part proof=req.getPart("idProof");String original=proof==null?"":Objects.toString(proof.getSubmittedFileName(),"");
        String ext=original.toLowerCase(Locale.ROOT).endsWith(".pdf")?".pdf":original.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg)$")?".jpg":original.toLowerCase(Locale.ROOT).endsWith(".png")?".png":"";
        String type=proof==null?"":Objects.toString(proof.getContentType(),"");
        if(name.isBlank()||mail.isBlank()||password==null||password.length()<8||phone.isBlank()||proof==null||proof.getSize()<1||ext.isBlank()||!(type.equals("application/pdf")||type.equals("image/jpeg")||type.equals("image/png"))){Json.send(resp,400,Json.error("Enter your name, email, phone, password and a PDF, JPG or PNG ID proof."));return;}
        String storage=System.getenv().getOrDefault("CAMPUSNEST_UPLOAD_DIR",Path.of(System.getProperty("user.dir"),"campusnest-private","owner-proofs").toString());Path directory=Path.of(storage).toAbsolutePath().normalize();Files.createDirectories(directory);String stored=UUID.randomUUID()+ext;Path target=directory.resolve(stored).normalize();if(!target.getParent().equals(directory)){Json.send(resp,400,Json.error("Invalid file."));return;}
        try(Connection c=Db.connect()){
            c.setAutoCommit(false);
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO users(full_name,email,phone,password_hash,role,status) VALUES(?,?,?,?,'OWNER','PENDING')",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,name);ps.setString(2,mail);ps.setString(3,phone);ps.setString(4,Passwords.hash(password));ps.executeUpdate();long id;try(ResultSet keys=ps.getGeneratedKeys()){keys.next();id=keys.getLong(1);}proof.write(target.toString());
                try(PreparedStatement doc=c.prepareStatement("INSERT INTO owner_documents(user_id,stored_name,original_name,media_type) VALUES(?,?,?,?)")){doc.setLong(1,id);doc.setString(2,stored);doc.setString(3,original.substring(Math.max(0,original.length()-180)));doc.setString(4,type);doc.executeUpdate();}c.commit();Map<String,Object> user=Map.of("id",id,"name",name,"email",mail,"role","OWNER","status","PENDING");Json.send(resp,201,Map.of("user",user,"message","Registration received. An admin must review your details."));
            }catch(Exception e){c.rollback();Files.deleteIfExists(target);if(e instanceof SQLIntegrityConstraintViolationException){Json.send(resp,409,Json.error("An account already uses that email."));return;}if(e instanceof IOException io)throw io;if(e instanceof SQLException sql)throw sql;throw new IOException(e);}finally{c.setAutoCommit(true);}
        }
    }

    private void createInquiry(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);long listing=num(b,"listingId",0);String name=str(b,"name"),phone=str(b,"phone"),message=str(b,"message");if(listing<1||name.isBlank()||phone.isBlank()||message.isBlank()||message.length()>2000){Json.send(resp,400,Json.error("Complete your name, phone number and message."));return;}
        Map<String,Object> user=sessionUser(req);try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO inquiries(listing_id,student_user_id,sender_name,sender_phone,message) SELECT p.id,?,?,?,? FROM pg_listings p WHERE p.id=? AND p.is_visible=TRUE AND p.moderation_status='APPROVED'",Statement.RETURN_GENERATED_KEYS)){
            ps.setObject(1,user==null?null:user.get("id"));ps.setString(2,name);ps.setString(3,phone);ps.setString(4,message);ps.setLong(5,listing);if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("This PG listing is no longer available."));return;}Json.send(resp,201,Map.of("ok",true,"message","Your inquiry was sent to the owner."));
        }
    }

    private void createFeedback(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String name=str(b,"name"),mail=email(str(b,"email")),message=str(b,"message");if(name.isBlank()||message.isBlank()||message.length()>2000){Json.send(resp,400,Json.error("Enter your name and feedback message."));return;}Map<String,Object> u=sessionUser(req);try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO feedback(user_id,name,email,message) VALUES(?,?,?,?)")){ps.setObject(1,u==null?null:u.get("id"));ps.setString(2,name);ps.setString(3,mail.isBlank()?null:mail);ps.setString(4,message);ps.executeUpdate();Json.send(resp,201,Map.of("ok",true));}
    }

    private static String trim(String s){return s==null?"":s.trim();}

    private List<Map<String,Object>> ownerListings(HttpServletRequest req)throws SQLException{
        long owner=((Number)sessionUser(req).get("id")).longValue();try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("SELECT "+PUBLIC_COLUMNS+",p.moderation_status,p.is_visible FROM pg_listings p LEFT JOIN users u ON u.id=p.owner_user_id WHERE p.owner_user_id=? ORDER BY p.updated_at DESC")){ps.setLong(1,owner);try(ResultSet rs=ps.executeQuery()){List<Map<String,Object>> a=new ArrayList<>();while(rs.next())a.add(listingMap(rs,true));return a;}}
    }
    private void createListing(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String name=str(b,"name"),gender=str(b,"gender"),room=str(b,"room"),address=str(b,"address"),phone=str(b,"phone"),ownerName=String.valueOf(sessionUser(req).get("name"));int rent=num(b,"rent",-1),total=num(b,"total",-1),vacant=num(b,"vacant",-1);double distance=decimal(b,"distance",-1);boolean food=Boolean.TRUE.equals(b.get("food"));List<?> amenityRaw=b.get("amenities") instanceof List<?> l?l:List.of();String amenities=Json.MAPPER.writeValueAsString(amenityRaw);
        if(name.isBlank()||!(gender.equals("Boys")||gender.equals("Girls"))||!(room.equals("Shared")||room.equals("Single"))||rent<1||total<1||vacant<0||vacant>total||distance<0||address.isBlank()||phone.isBlank()){Json.send(resp,400,Json.error("Check the PG details and bed counts."));return;}
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO pg_listings(owner_user_id,owner_name,name,gender,monthly_rent,room_type,food_available,beds_total,beds_vacant,distance_km,address,amenities_json,description,contact_phone,moderation_status,is_visible,is_demo) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PENDING',TRUE,FALSE)",Statement.RETURN_GENERATED_KEYS)){
            int i=1;ps.setObject(i++,sessionUser(req).get("id"));ps.setString(i++,ownerName);ps.setString(i++,name);ps.setString(i++,gender);ps.setInt(i++,rent);ps.setString(i++,room);ps.setBoolean(i++,food);ps.setInt(i++,total);ps.setInt(i++,vacant);ps.setDouble(i++,distance);ps.setString(i++,address);ps.setString(i++,amenities);ps.setString(i++,str(b,"desc"));ps.setString(i,phone);ps.executeUpdate();try(ResultSet keys=ps.getGeneratedKeys()){keys.next();Json.send(resp,201,Map.of("id",keys.getLong(1),"status","PENDING","message","Listing submitted for admin review."));}
        }
    }
    private void updateListing(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String name=str(b,"name"),gender=str(b,"gender"),room=str(b,"room"),address=str(b,"address"),phone=str(b,"phone");int rent=num(b,"rent",-1),total=num(b,"total",-1),vacant=num(b,"vacant",-1);double distance=decimal(b,"distance",-1);if(name.isBlank()||!(gender.equals("Boys")||gender.equals("Girls"))||!(room.equals("Shared")||room.equals("Single"))||rent<1||total<1||vacant<0||vacant>total||distance<0||address.isBlank()||phone.isBlank()){Json.send(resp,400,Json.error("Check the PG details and bed counts."));return;}String amenities=Json.MAPPER.writeValueAsString(b.get("amenities") instanceof List<?> l?l:List.of());
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_listings SET name=?,gender=?,monthly_rent=?,room_type=?,food_available=?,beds_total=?,beds_vacant=?,distance_km=?,address=?,amenities_json=?,description=?,contact_phone=? WHERE id=? AND owner_user_id=?")){int i=1;ps.setString(i++,name);ps.setString(i++,gender);ps.setInt(i++,rent);ps.setString(i++,room);ps.setBoolean(i++,Boolean.TRUE.equals(b.get("food")));ps.setInt(i++,total);ps.setInt(i++,vacant);ps.setDouble(i++,distance);ps.setString(i++,address);ps.setString(i++,amenities);ps.setString(i++,str(b,"desc"));ps.setString(i++,phone);ps.setLong(i++,id);ps.setObject(i,sessionUser(req).get("id"));if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("PG listing not found."));return;}Json.send(resp,200,Map.of("ok",true));}
    }
    private void updateVacancy(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);int value=num(b,"vacant",-1);if(value<0){Json.send(resp,400,Json.error("Vacant beds must be zero or more."));return;}try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_listings SET beds_vacant=? WHERE id=? AND owner_user_id=? AND ?<=beds_total")){ps.setInt(1,value);ps.setLong(2,id);ps.setObject(3,sessionUser(req).get("id"));ps.setInt(4,value);if(ps.executeUpdate()==0){Json.send(resp,400,Json.error("Could not update vacancy. Check the listing and total beds."));return;}Json.send(resp,200,Map.of("ok",true,"vacant",value));}
    }
    private List<Map<String,Object>> ownerInquiries(HttpServletRequest req)throws SQLException{
        String sql="SELECT i.id,i.sender_name,i.sender_phone,i.message,i.status,i.created_at,p.name listing_name FROM inquiries i JOIN pg_listings p ON p.id=i.listing_id WHERE p.owner_user_id=? ORDER BY i.created_at DESC";try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement(sql)){ps.setObject(1,sessionUser(req).get("id"));try(ResultSet rs=ps.executeQuery()){List<Map<String,Object>> a=new ArrayList<>();while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("sender_name"));m.put("phone",rs.getString("sender_phone"));m.put("message",rs.getString("message"));m.put("status",rs.getString("status"));m.put("date",now(rs,"created_at"));m.put("listing",rs.getString("listing_name"));a.add(m);}return a;}}
    }
    private Map<String,Object> adminCounts()throws SQLException{
        try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT (SELECT COUNT(*) FROM users WHERE role='OWNER' AND status='PENDING') pending_owners,(SELECT COUNT(*) FROM pg_listings WHERE is_visible=TRUE AND moderation_status='APPROVED') visible_listings,(SELECT COUNT(*) FROM inquiries WHERE status='NEW') inquiries,(SELECT COUNT(*) FROM feedback WHERE status='NEW') feedback")){rs.next();return Map.of("pendingOwners",rs.getInt(1),"visibleListings",rs.getInt(2),"inquiries",rs.getInt(3),"feedback",rs.getInt(4));}
    }
    private List<Map<String,Object>> ownerQueue()throws SQLException{
        String sql="SELECT u.id,u.full_name,u.email,u.phone,u.status,u.created_at,d.original_name FROM users u LEFT JOIN owner_documents d ON d.user_id=u.id WHERE u.role='OWNER' ORDER BY FIELD(u.status,'PENDING','APPROVED','REJECTED'),u.created_at DESC";try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet rs=s.executeQuery(sql)){List<Map<String,Object>>a=new ArrayList<>();while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("full_name"));m.put("email",rs.getString("email"));m.put("phone",rs.getString("phone"));m.put("status",rs.getString("status"));m.put("date",now(rs,"created_at"));m.put("proof",rs.getString("original_name"));a.add(m);}return a;}
    }
    private void ownerProof(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        String stored=null,type=null;
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("SELECT stored_name,media_type FROM owner_documents WHERE user_id=?")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(rs.next()){stored=rs.getString(1);type=rs.getString(2);}}}
        if(stored==null){Json.send(resp,404,Json.error("Owner proof not found."));return;}
        String base=System.getenv().getOrDefault("CAMPUSNEST_UPLOAD_DIR",Path.of(System.getProperty("user.dir"),"campusnest-private","owner-proofs").toString());Path dir=Path.of(base).toAbsolutePath().normalize(),file=dir.resolve(stored).normalize();
        if(!file.getParent().equals(dir)||!Files.isRegularFile(file)){Json.send(resp,404,Json.error("Owner proof not found."));return;}
        String safeType=Set.of("application/pdf","image/jpeg","image/png").contains(type)?type:"application/octet-stream";
        resp.setHeader("Cache-Control","no-store");resp.setHeader("Content-Disposition","inline; filename=\"owner-proof\"");resp.setContentType(safeType);Files.copy(file,resp.getOutputStream());
    }
    private void updateOwnerStatus(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String status=str(b,"status");if(!Set.of("APPROVED","REJECTED").contains(status)){Json.send(resp,400,Json.error("Choose APPROVED or REJECTED."));return;}try(Connection c=Db.connect()){c.setAutoCommit(false);try(PreparedStatement ps=c.prepareStatement("UPDATE users SET status=? WHERE id=? AND role='OWNER'")){ps.setString(1,status);ps.setLong(2,id);if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("Owner not found."));return;}}try(PreparedStatement ps=c.prepareStatement("UPDATE owner_documents SET reviewed_at=CURRENT_TIMESTAMP WHERE user_id=?")){ps.setLong(1,id);ps.executeUpdate();}c.commit();Json.send(resp,200,Map.of("ok",true,"status",status));} }
    private void toggleListing(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);Object v=b.get("visible");if(!(v instanceof Boolean visible)){Json.send(resp,400,Json.error("visible must be true or false."));return;}try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_listings SET is_visible=? WHERE id=?")){ps.setBoolean(1,visible);ps.setLong(2,id);if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("Listing not found."));return;}Json.send(resp,200,Map.of("ok",true,"visible",visible));}
    }
    private void updateListingStatus(HttpServletRequest req,HttpServletResponse resp,long id)throws SQLException,IOException{
        Map<String,Object>b=Json.read(req);String status=str(b,"status");if(!Set.of("APPROVED","REJECTED").contains(status)){Json.send(resp,400,Json.error("Choose APPROVED or REJECTED."));return;}
        try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("UPDATE pg_listings SET moderation_status=?,is_visible=? WHERE id=?")){ps.setString(1,status);ps.setBoolean(2,status.equals("APPROVED"));ps.setLong(3,id);if(ps.executeUpdate()==0){Json.send(resp,404,Json.error("Listing not found."));return;}Json.send(resp,200,Map.of("ok",true,"status",status));}
    }
    private List<Map<String,Object>> feedback()throws SQLException{
        String sql="SELECT id,name,email,message,status,created_at FROM feedback ORDER BY created_at DESC";try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet rs=s.executeQuery(sql)){List<Map<String,Object>>a=new ArrayList<>();while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("name"));m.put("email",rs.getString("email"));m.put("message",rs.getString("message"));m.put("status",rs.getString("status"));m.put("date",now(rs,"created_at"));a.add(m);}return a;}
    }
}
