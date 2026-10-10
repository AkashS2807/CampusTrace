package com.example.campustrace;

import com.example.campustrace.entity.User;
import com.example.campustrace.model.Item;
import com.example.campustrace.repository.ItemRepository;
import com.example.campustrace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CampustraceApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ItemRepository items;
    @Autowired PasswordEncoder encoder;
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    @Test void embeddedServerServesRealHttp() throws Exception {
        var client=java.net.http.HttpClient.newHttpClient();
        var response=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:"+port+"/registeredUser")).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Sign In", "name=\"_csrf\"");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
    }
    @BeforeEach void clean() { items.deleteAll(); users.deleteAll(); }
    User account(String email,String role) {
        User u=new User(); u.setFullName("Demo Student"); u.setEmail(email);
        u.setPassword(encoder.encode("DemoPass123!")); u.setContactNumber("0000000000"); u.setRole(role);
        return users.saveAndFlush(u);
    }
    Item item(String name,String type,String owner) {
        Item i=new Item(); i.setItemName(name); i.setDescription("Blue cover near library");
        i.setType(type); i.setPostedBy(owner); i.setPostedByName("Demo Student");
        i.setContactNumber("0000000000"); i.setStatus("OPEN"); return items.saveAndFlush(i);
    }
    @Test void registrationHashesPasswordAndNormalizesEmail() throws Exception {
        mvc.perform(post("/saveUser").with(csrf()).param("fullName"," Demo Student ")
            .param("email"," Student@Example.Test ").param("password","DemoPass123!").param("contactNumber","0000000000"))
            .andExpect(redirectedUrl("/registeredUser"));
        User u=users.findByEmail("student@example.test");
        assertThat(u).isNotNull(); assertThat(u.getFullName()).isEqualTo("Demo Student");
        assertThat(encoder.matches("DemoPass123!",u.getPassword())).isTrue();
        assertThat(u.getRole()).isEqualTo("USER");
    }
    @Test void duplicateRegistrationIsRejected() throws Exception {
        account("student@example.test","USER");
        mvc.perform(post("/saveUser").with(csrf()).param("fullName","Demo").param("email","STUDENT@example.test")
            .param("password","DemoPass123!").param("contactNumber","0000000000"))
            .andExpect(view().name("register")).andExpect(model().attributeExists("error"));
        assertThat(users.count()).isEqualTo(1);
    }
    @Test void invalidRegistrationPreservesSafeInput() throws Exception {
        mvc.perform(post("/saveUser").with(csrf()).param("fullName"," ").param("email","bad")
            .param("password","short").param("contactNumber","       "))
            .andExpect(view().name("register")).andExpect(model().hasErrors())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"short\""))));
        assertThat(users.count()).isZero();
    }
    @Test void validLoginRotatesSessionAndLogoutProtectsPages() throws Exception {
        account("student@example.test","USER"); MockHttpSession before=new MockHttpSession(); String old=before.getId();
        var result=mvc.perform(post("/userLogin").session(before).with(csrf())
            .param("email","STUDENT@example.test").param("password","DemoPass123!"))
            .andExpect(authenticated().withUsername("student@example.test")).andExpect(redirectedUrl("/dashboard")).andReturn();
        MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);
        assertThat(session.getId()).isNotEqualTo(old);
        mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(unauthenticated()).andExpect(redirectedUrl("/?logout"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }
    @Test void storedAdminRoleGrantsAccessAfterRealLogin() throws Exception {
        account("admin@example.test","ADMIN");
        var result=mvc.perform(post("/userLogin").with(csrf()).param("email","admin@example.test")
            .param("password","DemoPass123!")).andExpect(authenticated().withRoles("ADMIN")).andReturn();
        MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);
        mvc.perform(get("/manageItems").session(session)).andExpect(status().isOk());
        mvc.perform(get("/allUsers").session(session)).andExpect(status().isOk());
    }
    @Test void wrongAndMissingCredentialsAreRejected() throws Exception {
        account("student@example.test","USER");
        for (String email:new String[]{"student@example.test","missing@example.test"}) {
            mvc.perform(post("/userLogin").with(csrf()).param("email",email).param("password","wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/registeredUser?error"));
        }
    }
    @Test void legacyPlainTextPasswordsAreRejected() throws Exception {
        User u=account("legacy@example.test","USER"); u.setPassword("DemoPass123!"); users.saveAndFlush(u);
        mvc.perform(post("/userLogin").with(csrf()).param("email",u.getEmail()).param("password","DemoPass123!"))
            .andExpect(unauthenticated()).andExpect(redirectedUrl("/registeredUser?error"));
    }
    @Test void anonymousCannotAccessProtectedPages() throws Exception {
        for (String path:new String[]{"/dashboard","/lostItem","/foundItem","/items","/search","/myPosts","/allUsers","/manageItems"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
        }
    }
    @Test void reportsPersistCorrectTypeAndAuthenticatedOwnership() throws Exception {
        account("student@example.test","USER");
        for (String type:new String[]{"Lost","Found"}) {
            mvc.perform(post("/save"+type+"Item").with(user("student@example.test")).with(csrf())
                .param("itemName",type+" Notebook").param("description"," Blue notebook ")
                .param("location"," Library ").param("contactNumber","0000000000")
                .param("postedBy","victim@example.test").param("status","RESOLVED"))
                .andExpect(redirectedUrl("/items"));
        }
        assertThat(items.count()).isEqualTo(2);
        assertThat(items.findAll()).extracting(Item::getType).containsExactlyInAnyOrder("LOST","FOUND");
        assertThat(items.findAll()).allSatisfy(i -> {
            assertThat(i.getPostedBy()).isEqualTo("student@example.test");
            assertThat(i.getStatus()).isEqualTo("OPEN"); assertThat(i.getLocation()).isEqualTo("Library");
        });
    }
    @Test void invalidReportsDoNotPersist() throws Exception {
        for (String path:new String[]{"/saveLostItem","/saveFoundItem"}) {
            mvc.perform(post(path).with(user("student@example.test")).with(csrf()).param("itemName"," ")
                .param("description","x".repeat(256)).param("contactNumber","not a phone"))
                .andExpect(status().isOk()).andExpect(model().hasErrors());
        }
        assertThat(items.count()).isZero();
    }
    @Test void combinedSearchFiltersAndEmptyResultsWork() throws Exception {
        Item lost=item("Notebook","LOST","student@example.test"); item("Notebook","FOUND","other@example.test");
        mvc.perform(get("/items").with(user("student@example.test")).param("keyword","BLUE").param("type","lost").param("status","open"))
            .andExpect(model().attribute("items",org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get("/search").with(user("student@example.test")).param("keyword","nonexistent"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("No items found")));
        mvc.perform(get("/items").with(user("student@example.test")).param("keyword"," "))
            .andExpect(model().attribute("items",org.hamcrest.Matchers.hasSize(2)));
        mvc.perform(get("/myPosts").with(user("student@example.test")))
            .andExpect(model().attribute("items",org.hamcrest.Matchers.contains(org.hamcrest.Matchers.hasProperty("id",org.hamcrest.Matchers.is(lost.getId())))));
    }
    @Test void ownerCanResolveAndRepeatIsHarmless() throws Exception {
        Item i=item("Notebook","LOST","student@example.test");
        for (int n=0;n<2;n++) mvc.perform(post("/resolve/"+i.getId()).with(user(i.getPostedBy())).with(csrf()))
            .andExpect(redirectedUrl("/items"));
        assertThat(items.findById(i.getId()).orElseThrow().getStatus()).isEqualTo("RESOLVED");
        mvc.perform(get("/items").with(user(i.getPostedBy())).param("status","RESOLVED"))
            .andExpect(model().attribute("items",org.hamcrest.Matchers.hasSize(1)));
    }
    @Test void anotherUserCannotResolveOrDelete() throws Exception {
        Item i=item("Notebook","LOST","owner@example.test");
        mvc.perform(post("/resolve/"+i.getId()).with(user("attacker@example.test")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/deleteItem/"+i.getId()).with(user("attacker@example.test")).with(csrf())).andExpect(status().isForbidden());
        assertThat(items.findById(i.getId()).orElseThrow().getStatus()).isEqualTo("OPEN");
    }
    @Test void normalUserCannotReadAdminPages() throws Exception {
        for (String path:new String[]{"/allUsers","/manageItems"}) mvc.perform(get(path).with(user("student@example.test"))).andExpect(status().isForbidden());
    }
    @Test void adminCanResolveAndDeleteOnlyTarget() throws Exception {
        account("admin@example.test","ADMIN"); Item first=item("Notebook","LOST","owner@example.test");
        Item second=item("Bottle","FOUND","owner@example.test");
        mvc.perform(post("/resolve/"+first.getId()).with(user("admin@example.test").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/manageItems").with(user("admin@example.test").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/allUsers").with(user("admin@example.test").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/deleteItem/"+first.getId()).with(user("admin@example.test").roles("ADMIN")).with(csrf())).andExpect(redirectedUrl("/manageItems"));
        assertThat(items.existsById(first.getId())).isFalse(); assertThat(items.existsById(second.getId())).isTrue();
    }
    @Test void missingItemsReturn404() throws Exception {
        mvc.perform(post("/resolve/99999").with(user("student@example.test")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/deleteItem/99999").with(user("admin@example.test").roles("ADMIN")).with(csrf())).andExpect(status().isNotFound());
    }
    @Test void mutationsRequireCsrfAndGetCannotMutate() throws Exception {
        Item i=item("Notebook","LOST","student@example.test");
        for (String path:new String[]{"/resolve/"+i.getId(),"/deleteItem/"+i.getId(),"/saveLostItem","/saveFoundItem","/logout","/saveUser","/userLogin"}) {
            mvc.perform(post(path).with(user("student@example.test").roles("ADMIN"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/resolve/"+i.getId()).with(user(i.getPostedBy()))).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/deleteItem/"+i.getId()).with(user(i.getPostedBy()).roles("ADMIN"))).andExpect(status().isMethodNotAllowed());
        assertThat(items.findById(i.getId()).orElseThrow().getStatus()).isEqualTo("OPEN");
    }
    @Test void publicAndProtectedTemplatesRenderWithCsrf() throws Exception {
        for (String path:new String[]{"/","/role","/userChoice","/newUser","/registeredUser","/admin"}) mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(get("/lostItem").with(user("student@example.test"))).andExpect(view().name("lostItem"));
        mvc.perform(get("/foundItem").with(user("student@example.test"))).andExpect(view().name("foundItem"));
        account("student@example.test","USER");
        for (String path:new String[]{"/dashboard","/lostItem","/foundItem","/items","/myPosts"})
            mvc.perform(get(path).with(user("student@example.test"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }
    @Test void invalidFiltersAndIdsAreRejected() throws Exception {
        mvc.perform(get("/items").with(user("student@example.test")).param("type","INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get("/items").with(user("student@example.test")).param("status","INVALID")).andExpect(status().isBadRequest());
        mvc.perform(post("/resolve/bad").with(user("student@example.test")).with(csrf())).andExpect(status().isBadRequest());
    }
    @Test void userContentIsEscaped() throws Exception {
        item("<script>alert(1)</script>","LOST","student@example.test");
        mvc.perform(get("/items").with(user("student@example.test")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
    }
}
