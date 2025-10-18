// src/app/app-routing.module.ts
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';

import { Login} from './login/login';
import { Home } from './home/home';
import { Register } from './register/register';
import { Search } from './search/search';
import { PlayerProfile } from './player-profile/player-profile';
import { Compare } from './compare/compare';
import { Dashboard } from './dashboard/dashboard';
export const routes: Routes =  [
  { path: '', component: Home }, // page d'accueil
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path : 'search', component: Search },
  { path : 'compare', component: Compare }, // page de comparaison
  {path : 'player/:id', component: PlayerProfile}, // page de profil joueur
  {path: 'dashboard', component: Dashboard}, // page de tableau de bord
  { path: '**', redirectTo: '' }, // rediriger vers home si chemin inconnu
];

@NgModule({
  imports: [RouterModule.forRoot(routes), ReactiveFormsModule],
  exports: [RouterModule]
})
export class AppRoutingModule { }
