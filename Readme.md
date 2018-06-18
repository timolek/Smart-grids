# Simulation study Smart Grid with multiple EVs

In this repository the codes and raw data can be found for the Smart grids project that studies the effect of EVs in a cluster of thousand households.

## Getting started
In the directionary called 'device agents' one can find the agents that are made for this project. The agents are used in combination with the PowerMatcher software which can be found over here: https://github.com/flexiblepower/powermatcher
These agents can be used in combination with the 'example's library of the Powermatcher software. They can be added in the 'src' folder of this library and used in combination with the other agents. For more information about how to setup the PowerMatcher software look at the github-page of the PowerMatcher software itself.

Note: the used for software for the simulations is not openly available. The agents can only be used in the current time.

In the directionary called 'codes to create patterns' one can find the codes that were used to calculate and write the demand data into a text-file that was used by three agents to make bids. In the directionary 'patterns' these created text files can be found. These text-files are nesecarry for the household-, solorpanal and windTurbine-agents to function properly.

## Configuration
The used configurations for the clusters for the simulations can be found in 'JSON configurations'. 

## Raw results
In 'raw simulation data' one can find all the text files that were created during the different simulations. In this directionary one can also find a runnable python file to combine all these text files into one text file.

## Data calculations and graphs
The data calculations that are done based on this raw data can be found in 'Data calculations'.



