#include <balanza.h>

void Balanza::begin(int data, int sck){
  Serial.println("Construyendo Balanza");
	b.begin(data,sck);
	b.set_scale(fcorreccion);
	b.tare(20);	
}

double Balanza::leerBalanza(){
//	double p = b.is_ready() ? (b.get_value(5)*1000)+1000 : -1;
	double r =  b.get_units(2)/100;//p > 0 ? p : 0;
	return r;
}

bool Balanza::isPesoAlcanzado(){
	pesoBalanza = leerBalanza();
	if (pesoRequerido == 0)
		pesoRequerido = pesoADepositar - pesoBalanza;

	return pesoRequerido > pesoBalanza;
}

double Balanza::setPesoADepositar(double p){
	return pesoADepositar = leerBalanza() - p;
}

double Balanza::leerPesoBalanza(){
	return pesoBalanza = leerBalanza();
}
/*
int Balanza::leerPesoBalanza(){
	return pesoBalanza=leerBalanza();
}*/