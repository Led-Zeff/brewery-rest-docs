package guru.springframework.msscbrewery.web.controller;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

import guru.springframework.msscbrewery.services.v2.BeerServiceV2;
import guru.springframework.msscbrewery.web.controller.v2.BeerControllerV2;
import guru.springframework.msscbrewery.web.model.v2.BeerDtoV2;
import guru.springframework.msscbrewery.web.model.v2.BeerStyleEnum;

@RunWith(SpringRunner.class)
@WebMvcTest(BeerControllerV2.class)
@AutoConfigureRestDocs
public class BeerControllerV2Test {

    @MockBean
    BeerServiceV2 beerService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    BeerDtoV2 validBeer;

    @Before
    public void setUp() {
        validBeer = BeerDtoV2.builder().id(UUID.randomUUID())
                .beerName("Beer1")
                .beerStyle(BeerStyleEnum.ALE)
                .upc(123456789012L)
                .build();
    }

    @Test
    public void getBeer() throws Exception {
        given(beerService.getBeerById(any(UUID.class))).willReturn(validBeer);

        mockMvc.perform(get("/api/v2/beer/{beerId}", validBeer.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(validBeer.getId().toString())))
                .andExpect(jsonPath("$.beerName", is("Beer1")))
                .andDo(MockMvcRestDocumentation.document("v2/beers-get", RequestDocumentation.pathParameters(
                    RequestDocumentation.parameterWithName("beerId").description("ID of desired beer to get.")
                ), PayloadDocumentation.responseFields(
                    PayloadDocumentation.fieldWithPath("id").description("Beers ID"),
                    PayloadDocumentation.fieldWithPath("beerName").description("Beers name"),
                    PayloadDocumentation.fieldWithPath("beerStyle").description("Beers stype"),
                    PayloadDocumentation.fieldWithPath("upc").description("Beers UPC")
                )));
    }

    @Test
    public void handlePost() throws Exception {
        //given
        BeerDtoV2 beerDto = validBeer;
        beerDto.setId(null);
        BeerDtoV2 savedDto = BeerDtoV2.builder().id(UUID.randomUUID()).beerName("New Beer").build();
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        given(beerService.saveNewBeer(any())).willReturn(savedDto);

        ConstraiedFields fields = new ConstraiedFields(BeerDtoV2.class);

        mockMvc.perform(post("/api/v2/beer/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
                .andExpect(status().isCreated())
                .andDo(MockMvcRestDocumentation.document("v2/beers-new", PayloadDocumentation.requestFields(
                    fields.withPath("id").description("Beer ID"),
                    fields.withPath("beerName").description("Beers name"),
                    fields.withPath("beerStyle").description("Beers stype"),
                    fields.withPath("upc").description("Beers UPC")
                )));

    }

    @Test
    public void handleUpdate() throws Exception {
        //given
        BeerDtoV2 beerDto = validBeer;
        beerDto.setId(null);
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        ConstraiedFields fields = new ConstraiedFields(BeerDtoV2.class);

        //when
        mockMvc.perform(put("/api/v2/beer/{beerId}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
                .andExpect(status().isNoContent())
                .andDo(MockMvcRestDocumentation.document("v2/beers-update", RequestDocumentation.pathParameters(
                    RequestDocumentation.parameterWithName("beerId").description("ID of desired beer to get.")
                ), PayloadDocumentation.requestFields(
                    fields.withPath("id").description("Beer ID"),
                    fields.withPath("beerName").description("Beers name"),
                    fields.withPath("beerStyle").description("Beers stype"),
                    fields.withPath("upc").description("Beers UPC")
                )));

        then(beerService).should().updateBeer(any(), any());

    }

    private static class ConstraiedFields {
        private final ConstraintDescriptions constraintDescriptions;
    
        ConstraiedFields(Class<?> input) {
          constraintDescriptions = new ConstraintDescriptions(input);
        }
    
        private FieldDescriptor withPath(String path) {
          return PayloadDocumentation.fieldWithPath(path).attributes(Attributes.key("constraints").value(
            StringUtils.collectionToDelimitedString(constraintDescriptions.descriptionsForProperty(path), ". ")
          ));
        }
      }
}