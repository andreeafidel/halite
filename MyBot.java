import hlt.*;
import java.util.*;
import java.io.*;

public class MyBot {

	public static void main(final String[] args) {
		final Networking networking = new Networking();
		final GameMap gameMap = networking.initialize("Dani Mocanu");

		final String initialMapIntelligence = "width: " + gameMap.getWidth() + "; height: " + gameMap.getHeight()
				+ "; players: " + gameMap.getAllPlayers().size() + "; planets: " + gameMap.getAllPlanets().size();
		Log.log(initialMapIntelligence);

		int turn = 0, aggro;

		final ArrayList<Move> moveList = new ArrayList<>();
		final ArrayList<Entity> onHold = new ArrayList<>();

		if (gameMap.getWidth() < 200)
			aggro = 1;
		else
			aggro = 7;

		Ship decoy = null;
		int decoyExistance = 0;
		int reachAlternateRoute = 0;
		
		/*try {
            Log2.initialize(new FileWriter(String.format("%d.log", gameMap.getMyPlayerId())));
        }
        catch (IOException e) {
            e.printStackTrace();
        }*/
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("Player" + gameMap.getMyPlayerId() , "UTF-8");
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		

		for (;;) {
			
			
			
			HashMap<Player, Position> enemy_center = new HashMap<>();
			GameMap.turn++;

			moveList.clear();
			networking.updateMap(gameMap);

			turn++;
			writer.println("TURN ---------- " + turn + "--------------");
			
			
			//verifica daca decoyul mai traieste
			writer.println("Decoy existance status before running the 'still alive' check: " + decoyExistance);
			
			if(decoyExistance == 1) {
				int flag = 0;
				for(final Ship ship : gameMap.getMyPlayer().getShips().values()) {
					writer.println("Comparing ship with id " + ship.getId() + " with decoy with id " + decoy.getId());
					if(ship.getId() == decoy.getId()) {
						writer.println("Decoy is still alive!");
						flag = 1;
						break;
					}
				}
				if(flag == 0) {
					writer.println("Did not found the decoy! Must be dead!");
					decoyExistance = 0;
					decoy = null;
				}
			}
			
			
			

			// calculeaza centrul de greutate al teritoriilor adverse
			for (Player player : gameMap.getAllPlayers()) {
				if (player.getId() != gameMap.getMyPlayerId()) {
					int x = 0, y = 0, count = 0;
					for (Planet planet : gameMap.getAllPlanets().values()) {
						if (planet.isOwned() && planet.getOwner() == player.getId()) {
							count++;
							x += planet.getXPos();
							y += planet.getYPos();
						}
					}
					if (count > 0)
						enemy_center.put(player, new Position(x / count, y / count));
					else
						enemy_center.put(player, new Position(gameMap.getWidth() / 2, gameMap.getHeight() / 2));
				}
			}

			int contor = 0;

			for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
				
				// trebuie resetat pentru fiecara nava
				// in caz contrar va adauga prea multe nave si va fi un numar eronat
				ArrayList<Ship> undockedShips = new ArrayList<>();

				
				int dest = 0; //indica daca o nava are o destinatie
				if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
					continue;
				}

				contor++;

				ArrayList<Planet> AllPlanets = new ArrayList<>();
				ArrayList<Ship> AllShips = new ArrayList<>();

				for (final Planet planet : gameMap.getAllPlanets().values()) {
					AllPlanets.add(planet);
				}

				Collections.sort(AllPlanets, new Comparator<Planet>() {

					@Override
					public int compare(Planet a, Planet b) {
						if (ship.getDistanceTo(a) <= ship.getDistanceTo(b) + 3.5
								&& ship.getDistanceTo(a) >= ship.getDistanceTo(b) - 3.5) {
							return (int) (a.getDockingSpots() - b.getDockingSpots());
						}
						return (int) (ship.getDistanceTo(a) - ship.getDistanceTo(b));
					}

				});

				for (final Ship ship2 : gameMap.getAllShips()) {
					if (ship != ship2)
						AllShips.add(ship2);
				}

				Collections.sort(AllShips, new Comparator<Ship>() {
					@Override
					public int compare(Ship a, Ship b) {
						if (ship.getDistanceTo(a) >= ship.getDistanceTo(b))
							return 1;
						return -1;
					}
				});

				for (Ship ship2 : gameMap.getMyPlayer().getShips().values()) {
					// if (ship2.getOwner() == gameMap.getMyPlayer().getId()) {
					if (ship2.getDockingStatus().toString().equals("Undocked")) {
						//if (!ship2.getDockingStatus().toString().equals("Docked"))
						//if (ship2.getDockingStatus().toString().equals("Undocked"))
							undockedShips.add(ship2);
					}
				}

				///////////////////////
				writer.println("Decoy existance status: " + decoyExistance);
				writer.println("Number of undocked ships: " + undockedShips.size() );
				if (decoyExistance == 0 && undockedShips.size() > 5) {
					writer.println("Creating a new decoy!");
					decoy = ship;
					reachAlternateRoute = 0;
					decoyExistance = 1;
				}

				if (decoyExistance == 1 && decoy.getId() == ship.getId()) {
					writer.println("Reaching alternate route status: " + reachAlternateRoute);
					writer.println("Decoy existance status should be 1: " + decoyExistance);
					Position dest2 = enemy_center.entrySet().iterator().next().getValue();
					Map<Double, Position> corners = new TreeMap<>();

					Position cornerNW = new Position(0, 0);
					Position cornerNE = new Position(gameMap.getWidth(), 0);
					Position cornerSW = new Position(0, gameMap.getHeight());
					Position cornerSE = new Position(gameMap.getWidth(), gameMap.getHeight());

					corners.put(cornerNW.getDistanceTo(dest2), cornerNW);
					corners.put(cornerNE.getDistanceTo(dest2), cornerNE);
					corners.put(cornerSW.getDistanceTo(dest2), cornerSW);
					corners.put(cornerSE.getDistanceTo(dest2), cornerSE);

					Position alternateRoute = new Position(0, 0);
					int cc = 0;
					for (Position lo : corners.values()) {
						cc++;
						if (cc == 2) {
							alternateRoute = lo;
							break;
						}
					}
					
					writer.println("Current distance from destination: " + ship.getDistanceTo(alternateRoute));
					
					//constanta de distanta va trebui setata in functie de dimensiunea hartii
					if (ship.getDistanceTo(alternateRoute) > 65 && reachAlternateRoute == 0) {
						
						final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship,
								alternateRoute, Constants.MAX_SPEED, true, Constants.MAX_NAVIGATION_CORRECTIONS,
								Math.PI / 180.0);
						if (newThrustMove != null) {
							moveList.add(newThrustMove);
						}
						continue;
					} else {
						reachAlternateRoute = 1;
						//writer.println("Reached destination with status : " + reachAlternateRoute);
						final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, dest2,
								Constants.MAX_SPEED, true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);

						if (newThrustMove != null) {
							moveList.add(newThrustMove);
						}
						continue;
					} /**/
					
				}

				/**/
				///////////////////////

				int alert = 0;// spune daca o nava e prea aproape ca sa dockeze
				for (Ship ship2 : AllShips) {
					if (ship2.getOwner() != gameMap.getMyPlayer().getId()) {
						if (ship.getDistanceTo(ship2) < 36) {
							alert = 1;
						}
						break;
					}
				}

				for (final Planet planet : AllPlanets) {
					if (alert == 1)
						break;

					if (!planet.isFull() && (!planet.isOwned() || planet.getOwner() == gameMap.getMyPlayerId())) {
						int speed = Constants.MAX_SPEED;

						dest = 1;
						planet.onTheWay++;

						if (ship.canDock(planet)) {
							moveList.add(new DockMove(ship, planet));
							break;
						}

						final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, speed);
						if (newThrustMove != null) {
							moveList.add(newThrustMove);
						}
						break;

					}
				}

				if (dest == 1)
					continue;

				for (final Ship ship2 : AllShips) {
					if (ship2.getOwner() != gameMap.getMyPlayer().getId()) {
						int speed = Constants.MAX_SPEED;

						// nava curenta va ataca o nava dockata
						if (contor % aggro == 0) {
							// daca e dockata mergi spre ea
							if (ship2.getDockingStatus().toString().equals("Docked")) {
								final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship,
										ship.getClosestPoint(ship2), speed, true, Constants.MAX_NAVIGATION_CORRECTIONS,
										Math.PI / 180.0);
								if (newThrustMove != null) {
									moveList.add(newThrustMove);
								}
								break;
							}
							// altfel cauta o alta nava dockata ca si tinta;
							else
								continue;
						} else {
							if (ship.getHealth() > ship2.getHealth()) {
								final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship,
										ship.getClosestPoint(ship2), speed, true, Constants.MAX_NAVIGATION_CORRECTIONS,
										Math.PI / 180.0);
								if (newThrustMove != null) {
									moveList.add(newThrustMove);
								}
								break;
							}

							else {
								final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship,
										ship2, speed, true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);

								// ship2.onTheWay++;

								if (newThrustMove != null) {
									moveList.add(newThrustMove);
								}
								break;
							} /**/
						}
					}
				}
			}
			Networking.sendMoves(moveList);
			if(turn > 220)
				writer.close();
				
		}
		
	}
}
