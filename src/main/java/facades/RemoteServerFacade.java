/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.characterDTO;
import dto.filmDTO;
import dto.combinedDTO;
import dto.planetDTO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.persistence.EntityManagerFactory;
import utils.HttpUtils;

/**
 *
 * @author Patrick
 */
public class RemoteServerFacade {
    
      private static EntityManagerFactory emf;
    private static RemoteServerFacade instance;
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    
    
    
    public RemoteServerFacade(){}
      /**
     *
     * @param _emf
     * @return the instance of this facade.
     */
    public static RemoteServerFacade getRemoteServerFacade(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new RemoteServerFacade();
        }
        return instance;
    }
    
    
    public String getAllFilms() throws IOException{
        
        long start = System.nanoTime();
        String filmsJson = HttpUtils.fetchData("https://swapi.dev/api/films/1/");

        filmDTO filmsdto = GSON.fromJson(filmsJson, filmDTO.class);
        
         List<characterDTO> allCharacter = new ArrayList(); 
         List<planetDTO> allPlanets = new ArrayList(); 
         
        for (String ch : filmsdto.getCharacters()){
           String ch2 = ch.replace("http", "https");
            String data = HttpUtils.fetchData(ch2);
        characterDTO c = GSON.fromJson(data, characterDTO.class);
           allCharacter.add(c);
        }
        
         for (String planet : filmsdto.getPlanets()){
           String planet2 = planet.replace("http", "https");
            String data = HttpUtils.fetchData(planet2);
        planetDTO p = GSON.fromJson(data, planetDTO.class);
           allPlanets.add(p);
        }
      
         combinedDTO combined = new combinedDTO(filmsdto, allCharacter, allPlanets);
          long end = System.nanoTime();
          System.out.println((end - start));
        return GSON.toJson(combined);
    }
    
    
     public String getAllFilmsParallel() throws IOException, InterruptedException, ExecutionException{
         
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<String>> planetFutures = new ArrayList<>();
        List<Future<String>> characterFutures = new ArrayList<>();
        long start = System.nanoTime();
        String filmsJson = HttpUtils.fetchData("https://swapi.dev/api/films/1/");

        filmDTO filmsdto = GSON.fromJson(filmsJson, filmDTO.class);
        
         List<characterDTO> allCharacter = new ArrayList();
         List<planetDTO> planets = new ArrayList<>();
         
        for (String url : filmsdto.getPlanets()){
            Future future = executor.submit(new PlanetHandler(url));
            planetFutures.add(future);
        }
        
         for (String url : filmsdto.getCharacters()){
            Future future = executor.submit(new CharacterHandler(url));
            characterFutures.add(future);
        }
         
         for (Future f : characterFutures){
                 allCharacter.add((characterDTO) f.get());
         }
           for (Future f : planetFutures){
                 planets.add((planetDTO) f.get());        
         }
       
      
         combinedDTO combined = new combinedDTO(filmsdto, allCharacter, planets);
         long end = System.nanoTime();
          System.out.println("Parallel: " + (end - start) );
        return GSON.toJson(combined);
    }
    
    public class PlanetHandler implements Callable<planetDTO>{

        String planetUrl;
        public PlanetHandler(String planetUrl){
            this.planetUrl = planetUrl.replace("http", "https");
        }
        
        @Override
        public planetDTO call() throws Exception {
     
            String data = HttpUtils.fetchData(planetUrl);
            planetDTO planetdto = GSON.fromJson(data, planetDTO.class);
                
            return planetdto;
        }
        
    }
    
     public class CharacterHandler implements Callable<characterDTO>{

        String characteUrl;
        public CharacterHandler(String characteUrl){
            this.characteUrl = characteUrl.replace("http", "https");
        }
        
        @Override
        public characterDTO call() throws Exception {
     
            String data = HttpUtils.fetchData(characteUrl);
            characterDTO charactedto = GSON.fromJson(data, characterDTO.class);
                
            return charactedto;
        }
        
    }
}
